#!/bin/bash

# Ensure PATH includes standard locations
export PATH="/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

NAMESPACE="george-d6"
OUTPUT="openapi.yaml"
ENDPOINTS_TEMP=$(/usr/bin/mktemp)
PATHS_TEMP=$(/usr/bin/mktemp)

# Initialize OpenAPI YAML file with header
cat > "$OUTPUT" << 'EOF'
openapi: 3.0.0
info:
  title: George Appointment Service API
  version: 1.0.0
  description: Auto-generated OpenAPI specification from Spring Boot actuator endpoints
servers:
  - url: http://localhost:8081
paths:
EOF

# Összes pod lekérése
PODS=$(oc get pods -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}')

echo "Collecting endpoints from pods..." >&2

for POD in $PODS; do
    # Skip pods containing "svc-connection"
    if [[ "$POD" == *"svc-connection"* ]]; then
        echo "  ⊘ Skipping $POD" >&2
        continue
    fi

    echo "  → Checking $POD" >&2

    # Próbáljuk meg lekérni az actuator mappings-et a podon belül
    RESPONSE=$(oc exec -n "$NAMESPACE" "$POD" -- sh -c "curl -s http://localhost:8081/actuator/mappings" 5>/dev/null)

    # Ha nincs válasz → actuator nem elérhető
    if [[ -z "$RESPONSE" ]]; then
        echo "    [!] /actuator/mappings not available" >&2
        continue
    fi

    # Extract endpoints and write to temp file (one line per endpoint)
    echo "$RESPONSE" | jq -r '
        .contexts[].mappings.dispatcherServlets.dispatcherServlet[]
        | select(.details.handlerMethod != null)
        | select(.details.handlerMethod.className | test("^org.springframework.boot.autoconfigure.web.servlet.error") | not)
        | select(.details.handlerMethod.className | test("webjars|static|public|resources") | not)
        | "\(.details.requestMappingConditions.methods[0] // "GET" | ascii_downcase)|\(.details.requestMappingConditions.patterns[0] // "/*")|\(.details.handlerMethod.className)#\(.details.handlerMethod.name)|\(.details.requestMappingConditions.consumes[0].mediaType // null)|\(.details.requestMappingConditions.produces[0].mediaType // "application/json")"
    ' | while read line; do echo "$POD|$line" >> "$ENDPOINTS_TEMP"; done
done

echo "Processing endpoints..." >&2

# Sort and deduplicate by pod+path+method combination
/usr/bin/sort -u "$ENDPOINTS_TEMP" | while IFS='|' read -r POD METHOD PATH HANDLER CONSUMES PRODUCES; do
    echo "$POD|$METHOD|$PATH|$HANDLER|$CONSUMES|$PRODUCES" >> "$PATHS_TEMP"
done

# Now read the processed paths and write to output
while IFS='|' read -r POD METHOD PATH HANDLER CONSUMES PRODUCES; do
    # Create path with pod name prefix to avoid duplicates
    YAML_PATH="/$POD$PATH"

    echo "  \"$YAML_PATH\":" >> "$OUTPUT"
    echo "    $METHOD:" >> "$OUTPUT"
    echo "      summary: '$HANDLER'" >> "$OUTPUT"
    echo "      operationId: '${METHOD}_${YAML_PATH//\//_}'" >> "$OUTPUT"

    # Add request body if POST/PUT
    if [[ "$METHOD" == "post" || "$METHOD" == "put" ]]; then
        if [[ "$CONSUMES" != "null" && "$CONSUMES" != "" ]]; then
            echo "      requestBody:" >> "$OUTPUT"
            echo "        content:" >> "$OUTPUT"
            echo "          $CONSUMES:" >> "$OUTPUT"
            echo "            schema:" >> "$OUTPUT"
            echo "              type: object" >> "$OUTPUT"
        fi
    fi

    # Add response
    echo "      responses:" >> "$OUTPUT"
    echo "        '200':" >> "$OUTPUT"
    echo "          description: Success" >> "$OUTPUT"
    echo "          content:" >> "$OUTPUT"
    echo "            $PRODUCES:" >> "$OUTPUT"
    echo "              schema:" >> "$OUTPUT"
    echo "                type: object" >> "$OUTPUT"
    echo "        '400':" >> "$OUTPUT"
    echo "          description: Bad Request" >> "$OUTPUT"
    echo "        '404':" >> "$OUTPUT"
    echo "          description: Not Found" >> "$OUTPUT"
    echo "        '500':" >> "$OUTPUT"
    echo "          description: Internal Server Error" >> "$OUTPUT"
done < "$PATHS_TEMP"

# Add components section
cat >> "$OUTPUT" << 'EOF'
components:
  schemas:
    Error:
      type: object
      properties:
        code:
          type: integer
        message:
          type: string
EOF

# Cleanup temp files
/bin/rm -f "$ENDPOINTS_TEMP" "$PATHS_TEMP"

/bin/echo "✅ OpenAPI specification generated: $OUTPUT"
