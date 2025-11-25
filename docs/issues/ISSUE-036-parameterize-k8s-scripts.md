# ISSUE-036: Extract Spring Boot Actuator Endpoints from Kubernetes Namespace

**Created**: 2025-11-25
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Infrastructure | Developer Experience
**Blocking**: None

---

## Problem

We need reusable scripts to extract REST endpoint information from Spring Boot microservices running in Kubernetes namespaces. These scripts should query the `/actuator/mappings` endpoint across all pods in a namespace and aggregate the results in different formats (text report, OpenAPI specification, single pod details).

Currently, this capability is manual or uses hardcoded namespace references. We need general-purpose tools that accept a namespace parameter and produce standardized output.

---

## Root Cause

There's no standardized way to discover and document REST endpoints from a live Kubernetes namespace with multiple Spring Boot services. Manual inspection of each pod is error-prone and not scalable.

---

## Approach

### Script Descriptions (for Agent Implementation)

#### openapi.sh
**Purpose**: Generate OpenAPI 3.0 YAML specification from Spring Boot actuator `/actuator/mappings` endpoints across all pods in a Kubernetes namespace.

**Functionality**:
1. Accept Kubernetes namespace as CLI parameter (required)
2. Accept optional output filename (default `openapi.yaml`)
3. Iterate through all pods in the namespace
4. Skip pods matching exclusion patterns (e.g., `svc-connection` pods, init containers)
5. Execute `curl http://localhost:8081/actuator/mappings` on each pod via `oc exec`
6. Extract REST endpoints using jq filter (exclude error handlers, static resources, webjars)
7. Deduplicate endpoints by pod+method+path combination
8. Generate valid OpenAPI 3.0 YAML with:
   - Paths prefixed with pod name (e.g., `/pod-name/appointments`)
   - HTTP methods (GET, POST, PUT, DELETE, etc.)
   - Request/response content types
   - Standard HTTP response codes (200, 400, 404, 500)
   - Handler class and method names in summaries
9. Output to file with success confirmation

**Key Implementation Points**:
- Use `oc get pods -n <namespace>` to list available pods
- Use `sort -u` for deduplication (based on POD|METHOD|PATH|HANDLER|CONSUMES|PRODUCES)
- Quote YAML paths to handle special characters (`*`, `{`, `}`)
- Use full paths for commands (`/usr/bin/sort`, `/bin/cat`, etc.) to avoid PATH issues
- Use temp files to avoid subshell variable persistence issues
- Progress feedback to stderr
- Handle missing/unavailable actuator gracefully

#### single-pod.sh
**Purpose**: Extract and display REST endpoints from a single Kubernetes pod's Spring Boot actuator.

**Functionality**:
1. Accept Kubernetes namespace as CLI parameter (required)
2. Accept pod name as second parameter, or list available pods if not provided
3. Execute `oc exec` to curl actuator mappings on specific pod
4. Extract REST endpoints using same jq filter as openapi.sh
5. Filter out error handlers and static resources
6. Display formatted output with:
   - HTTP METHOD /path -> class#method
   - Sorted alphabetically by path
   - Save to file and display to console

**Key Implementation Points**:
- Provide clear usage message with syntax
- List available pods when invoked without pod name
- Sort output for readability and consistency
- Use same jq filter as openapi.sh for consistency
- Output to file and stdout simultaneously (via tee)

#### endpoints.sh
**Purpose**: Extract REST endpoints from all pods in a Kubernetes namespace and compile into text report.

**Functionality**:
1. Accept Kubernetes namespace as CLI parameter (required)
2. Iterate through all pods in namespace
3. Skip pods matching exclusion patterns (e.g., `svc-connection`)
4. Extract endpoints using same jq filter as openapi.sh
5. Format output as: `METHOD /path -> class#method` (sorted by pod, then path)
6. Output to text file with clear pod section headers
7. Clearly indicate skipped pods

**Key Implementation Points**:
- Consistent output format across all three scripts
- Same jq filter for endpoint extraction
- Progress feedback during execution

---

### Implementation Pattern

```bash
#!/bin/bash

# Ensure standard tools are in PATH
export PATH="/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

# Parameters - namespace is required
NAMESPACE="${1}"
OUTPUT="${2:-output.txt}"

# Validation
if [[ -z "$NAMESPACE" ]]; then
    echo "Usage: $0 <namespace> [output-file]"
    echo "Example: $0 my-namespace endpoints.yaml"
    echo ""
    echo "Available namespaces:"
    oc get ns -o jsonpath='{.items[*].metadata.name}' | tr ' ' '\n'
    exit 1
fi

# Shared jq Filter (consistent across all three scripts)
# Extracts: method|path|handler|consumes|produces
jq -r '
    .contexts[].mappings.dispatcherServlets.dispatcherServlet[]
    | select(.details.handlerMethod != null)
    | select(.details.handlerMethod.className | test("^org.springframework.boot.autoconfigure.web.servlet.error") | not)
    | select(.details.handlerMethod.className | test("webjars|static|public|resources") | not)
    | "\(.details.requestMappingConditions.methods[0] // "GET" | ascii_downcase)|\(.details.requestMappingConditions.patterns[0] // "/*")|\(.details.handlerMethod.className)#\(.details.handlerMethod.name)|\(.details.requestMappingConditions.consumes[0].mediaType // null)|\(.details.requestMappingConditions.produces[0].mediaType // "application/json")"
'
```

---

## Success Criteria

- [ ] `openapi.sh` accepts namespace as first CLI argument with default `george-d6`
- [ ] `single-pod.sh` accepts namespace as parameter (or uses environment variable)
- [ ] Both scripts validate namespace is not empty
- [ ] Help/usage text clearly documents the parameters
- [ ] Scripts work correctly with different namespaces
- [ ] No breaking changes to existing behavior (defaults work)
- [ ] Documentation updated in script headers

---

## Acceptance Tests

```bash
# Test 1: Default namespace behavior
./scripts/openapi.sh
# Should use george-d6 namespace

# Test 2: Custom namespace
./scripts/openapi.sh my-custom-namespace
# Should use my-custom-namespace

# Test 3: Help text exists
./scripts/openapi.sh --help
# Should show usage documentation

# Test 4: Validation
./scripts/openapi.sh ""
# Should fail gracefully with error message
```

---

## Related Issues

- Related: Script improvements for reusability
- Enhances: Previous work on openapi.sh and single-pod.sh

---

## Notes

- Consider adding optional output file parameter as well for full flexibility
- May benefit from environment variable fallback: `KUBE_NAMESPACE` env var
- Should follow similar pattern to `endpoints.sh` if it's also updated
