#!/bin/bash

NAMESPACE="george-d6"
POD_NAME="${1:-}"
OUTPUT="single-pod-output.txt"

# Check if pod name is provided
if [[ -z "$POD_NAME" ]]; then
    echo "Usage: $0 <pod-name>"
    echo ""
    echo "Available pods in namespace '$NAMESPACE':"
    oc get pods -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}' | tr ' ' '\n' | nl
    exit 1
fi

echo "Actuator REST endpoint gyűjtés - Pod: $POD_NAME (Namespace: $NAMESPACE)" > "$OUTPUT"
echo "-------------------------------------------------------" >> "$OUTPUT"
echo "Pod: $POD_NAME" | tee -a "$OUTPUT"
echo "----------------------------------------" >> "$OUTPUT"

# Try to get actuator mappings from the specified pod
RESPONSE=$(oc exec -n "$NAMESPACE" "$POD_NAME" -- sh -c "curl -s http://localhost:8081/actuator/mappings" 5>/dev/null)

# If no response → actuator not available
if [[ -z "$RESPONSE" ]]; then
    echo "  [!] /actuator/mappings not available on this pod." | tee -a "$OUTPUT"
    exit 1
fi

# Filter endpoints from JSON - extract REST endpoints only
echo "$RESPONSE" | jq -r '
    .contexts[].mappings.dispatcherServlets.dispatcherServlet[]
    | select(.details.handlerMethod != null)
    | select(.details.handlerMethod.className | test("^org.springframework.boot.autoconfigure.web.servlet.error") | not)
    | select(.details.handlerMethod.className | test("webjars|static|public|resources") | not)
    | "\(.details.requestMappingConditions.methods[0] // "ANY") \(.details.requestMappingConditions.patterns[0] // "/*") -> \(.details.handlerMethod.className)#\(.details.handlerMethod.name)"
' | sort | tee -a "$OUTPUT"

echo ""
echo "✅ Output saved to: $OUTPUT"
