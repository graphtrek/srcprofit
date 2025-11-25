#!/bin/bash

NAMESPACE="george-d6"
OUTPUT="endpoints.txt"

echo "Actuator REST endpoint gyűjtés - Namespace: $NAMESPACE" > "$OUTPUT"
echo "-------------------------------------------------------" >> "$OUTPUT"

# Összes pod lekérése
PODS=$(oc get pods -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}')

for POD in $PODS; do
    # Skip pods containing "svc-connection"
    if [[ "$POD" == *"svc-connection"* ]]; then
        echo "⊘ Pod: $POD (skipped)" | tee -a "$OUTPUT"
        echo "----------------------------------------" >> "$OUTPUT"
        echo "  [⊘] Skipped (svc-connection pod)" >> "$OUTPUT"
        echo "" >> "$OUTPUT"
        continue
    fi

    echo ">>> Pod: $POD" | tee -a "$OUTPUT"
    echo "----------------------------------------" >> "$OUTPUT"

    # Próbáljuk meg lekérni az actuator mappings-et a podon belül
    RESPONSE=$(oc exec -n "$NAMESPACE" "$POD" -- sh -c "curl -s http://localhost:8081/actuator/mappings" 5>/dev/null)

    # Ha nincs válasz → actuator nem elérhető
    if [[ -z "$RESPONSE" ]]; then
        echo "  [!] Nem elérhető a /actuator/mappings ezen a podon." | tee -a "$OUTPUT"
        echo "" >> "$OUTPUT"
        continue
    fi

    # Filter endpoints from JSON - extract REST endpoints only
    echo "$RESPONSE" | jq -r '
        .contexts[].mappings.dispatcherServlets.dispatcherServlet[]
        | select(.details.handlerMethod != null)
        | select(.details.handlerMethod.className | test("^org.springframework.boot.autoconfigure.web.servlet.error") | not)
        | select(.details.handlerMethod.className | test("webjars|static|public|resources") | not)
        | "\(.details.requestMappingConditions.methods[0] // "ANY") \(.details.requestMappingConditions.patterns[0] // "/*") -> \(.details.handlerMethod.className)#\(.details.handlerMethod.name)"
    ' | sort >> "$OUTPUT"

    echo "" >> "$OUTPUT"
done

echo "Kész. Eredmény: $OUTPUT"