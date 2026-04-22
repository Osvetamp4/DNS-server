# CS4700 Project 6: DNS Server

## High-Level Approach

The server is built around three core functions: `determine_response`, `handle_dns_lookup`, and `process_socket_response`.

**`determine_response`** cleanly separates the two main responsibilities of the server. If a query is for our authoritative domain, it answers directly from the zone file. If the query is from a local client asking about an external domain, it kicks off a recursive lookup. Non-local clients are rejected with REFUSED. This separation kept the logic clean and made it easy to reason about what each code path was doing.

**`handle_dns_lookup`** initiates a recursive resolution by creating a fresh UDP socket for the upstream communication and "pinning" it to a central dictionary (`socket_to_port_tracker`). That socket is then added to the `select.select` list in `run()`, so the main loop naturally monitors it alongside the primary socket without any threading. The dictionary entry holds a JSON-like object tracking all state for that particular lookup: the client's address, the original request, the list of NS IPs tried so far, the current index, any accumulated CNAME chain records, and retry timing. From the very beginning we built with parallelism in mind, so the architecture didn't need to change much once we reached that stage in the implementation, it was already there.

**`process_socket_response`** handles every response that comes back on a temp socket. It drives the iterative resolution forward: if the upstream server returned an answer, it packages it up and sends it back to the client. If it returned an NS referral in the authority section, it extracts the glue record and sends the next hop query. If no glue was present, it either checks the cache or kicks off a sub-resolution to find the NS hostname's IP.

## Challenges

**Bailiwick checking** was the trickiest part to get right. The initial implementation used the question's `qname` as the bailiwick domain, which was wrong for a query like `www.foo.`, the bailiwick should be `foo.` (from the authority section's NS rname), not `www.foo.`. This caused the server to reject all valid glue records and return SERVFAIL for every recursive query until we fixed the `_get_bailiwick` helper to read the NS/SOA rname from the authority section directly.

**CNAME chain resolution** had several subtle bugs. The chain head detection was incorrect. The old logic tried to walk a backwards map and found the wrong starting record. We rewrote it to identify the head by finding CNAME records whose `rname` is not pointed to by any other record in the set, then walk forward using a normalized string dictionary. We also had to handle cross-zone CNAME chains (`server.bar.foo CNAME other.baz.`) by restarting resolution from root with a fresh question for the CNAME target's name.

**Cache key normalization** was a subtle Python bug. `DNSLabel` objects don't hash consistently as dictionary keys when constructed from different sources, so cache lookups were silently missing. Normalizing all keys to `(str.rstrip(".").lower(), int)` tuples fixed the issue. We also learned that TTL=0 records should never be cached — serving them on repeat queries bypassed the intended behavior of always doing a fresh upstream lookup.

**Timeout and retry logic** required tracking the `current_question` separately from the `request_object`. The original request is the client's query and never changes, but the question we send upstream varies as we walk the NS chain. Retrying with the wrong question caused the upstream server to respond for a different name than expected.

## Design Properties We Think Are Good

**Task-board model for parallel processing.** Each temp socket maps to an isolated state object in `socket_to_port_tracker`. The main loop treats this like a task board — `select.select` wakes it up when any socket has data, and `process_socket_response` picks up exactly where that lookup left off. This model is simple to reason about and scales naturally: dozens of in-flight lookups coexist without interfering because each one's state is fully encapsulated in its own dictionary entry.

**Per-socket state flexibility.** Because each temp socket carries its own JSON-like state object, we could adapt behavior mid-resolution without affecting any other in-flight queries. Adding fields like `CNAME_chain`, `resolve_for_socket`, or `retry_count` required no global changes — just an extra key in one dictionary entry. This made iterating on edge cases fast.

**Sub-resolution for missing glue.** When an NS referral arrives with no glue record, rather than giving up with SERVFAIL, the server spins up a second temp socket to recursively resolve the NS hostname's A record, then feeds the result back to the original socket via `resolve_for_socket`. This keeps the main resolution alive and handles real world DNS topologies where glue isn't always provided

**Clean separation of authoritative vs. recursive paths.** The authoritative path handles CNAME chaining, NS delegation, MX, TXT, and NXDOMAIN entirely from the zone file. The recursive path is completely independent. Neither path touches the other's logic, which made debugging much easier

**Aggressive caching with TTL enforcement.** All in-bailiwick records from authority and additional sections are cached during recursive resolution, not just the final answer. This means subsequent queries for related names (e.g. the same NS server's A record) get cache hits instead of re-doing full recursive lookups. TTL is tracked as elapsed wall-clock time from when the record was stored, and TTL=0 records are never cached

## Testing

We used the provided `run` script against all config files in the `configs/` directory and wrote a `run_all.sh` wrapper that runs every test in sequence and prints a pass/fail summary with failure details

For debugging individual failures, we ran single tests with full output piped through `grep` to isolate the "Mismatched response" blocks showing expected vs. received. This made it possible to quickly identify whether a failure was a wrong record, a missing record, a wrong flag, or a timeout

We tested the different feature levels in order. Local authoritative queries first, then single-hop recursive, then multi-hop, then parallel clients, then bailiwick, then caching, then drop/delay following the implementation strategy in the spec. Each level exposed a new class of bugs: bailiwick revealed the `_get_bailiwick` logic error, caching revealed the `DNSLabel` hashing issue, and the drop test revealed the TTL=0 caching bug
