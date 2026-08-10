/*
 * AIGEM freshness check — is the governance state fresh enough for agents to trust?
 *
 * Run it:   java scripts/governance/FreshnessCheck.java
 *           ./gradlew governanceFreshness
 *
 * Requires nothing beyond the documented baseline: JDK 21 (single-file source
 * execution) and Git. No build step, no dependencies, works on Windows. That is
 * deliberate — a mandatory check must run on the runtime the repository already
 * declares, not on an optional one.
 *
 * FAILS CLOSED. Anything this program cannot fully understand is reported as a
 * halt, never skipped. See docs/governance/RUNBOOK.md section 4.5.
 *
 * Exit codes:
 *   0  fresh                — agents may run the full pipeline
 *   1  warnings             — agents proceed, but must state the warning in replies
 *   2  HALT-class staleness — agents must not ADMIT new work (may still PARK / REJECT)
 *
 * Options:
 *   --today YYYY-MM-DD   override today's date (testing)
 *   --quiet              print only problems
 *   --no-color           disable ANSI colour
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FreshnessCheck {

    // ---------------------------------------------------------------- config

    static final String STATE_FILE = "docs/governance/state/CURRENT-STATE.yaml";

    /** path, owner, maxAgeDays, halting */
    record Artefact(String path, String owner, int maxAgeDays, boolean halting) {}

    static final List<Artefact> ARTEFACTS = List.of(
        new Artefact(STATE_FILE,                                             "Delivery Lead", 30, true),
        new Artefact("docs/governance/01-CURRENT_STATE.md",                  "Delivery Lead", 30, true),
        new Artefact("docs/governance/04-STAGE_GATES.md",                    "Architect",     14, false),
        new Artefact("docs/governance/02-PROJECT_SCOPE.md",                  "Product Owner", 90, false),
        new Artefact("docs/governance/registers/SUGGESTION-REGISTER.md",     "triage owner",   7, false),
        new Artefact("docs/governance/registers/PARKED-BACKLOG.md",          "Delivery Lead", 90, false),
        new Artefact("docs/governance/registers/DEPENDENCY-REGISTER.md",     "Tech Lead",     14, false),
        new Artefact("docs/governance/registers/RISK-REGISTER.md",           "Delivery Lead", 90, false),
        new Artefact("docs/governance/registers/ASSUMPTION-REGISTER.md",     "authors",       90, false),
        new Artefact("docs/1sb-insurance-integration/service-ssot/TECH-DEBT.md", "Tech Lead", 90, false)
    );

    static final List<String> HALTS = new ArrayList<>();
    static final List<String> WARNS = new ArrayList<>();

    static Path root;
    static Path overrideRoot;          // --root, for fixture tests
    static boolean quiet, colour = true;

    static String g(String s) { return colour ? "[32m" + s + "[0m" : s; }
    static String y(String s) { return colour ? "[33m" + s + "[0m" : s; }
    static String r(String s) { return colour ? "[31m" + s + "[0m" : s; }

    static void halt(String msg) { HALTS.add(msg); System.out.println("  " + r("HALT") + "  " + msg); }
    static void warn(String msg) { WARNS.add(msg); System.out.println("  " + y("WARN") + "  " + msg); }
    static void pass(String msg) { if (!quiet) System.out.println("  " + g("OK  ") + "  " + msg); }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) throws Exception {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--today"    -> today = LocalDate.parse(args[++i]);
                case "--quiet"    -> quiet = true;
                case "--no-color" -> colour = false;
                case "--root"     -> overrideRoot = Paths.get(args[++i]).toAbsolutePath();
                case "--help"     -> { System.out.println("java FreshnessCheck.java [--today YYYY-MM-DD] [--root DIR] [--quiet] [--no-color]"); return; }
                default           -> { System.out.println("unknown option: " + args[i]); System.exit(2); }
            }
        }
        System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, StandardCharsets.UTF_8));
        root = overrideRoot != null ? overrideRoot : findRoot();
        System.out.println("AIGEM freshness check - " + today + "\n");

        Map<String, Object> state = checkStateFile(today);
        if (state != null) {
            checkStructure(state);
            checkIdAllocation(state);
        }
        checkArtefactAges(today);
        checkIdUniqueness();

        System.out.println();
        if (!HALTS.isEmpty()) {
            System.out.println(r("VERDICT: HALT-CLASS STALENESS"));
            HALTS.forEach(h -> System.out.println("  - " + h));
            System.out.println("\nAgents: do NOT admit new work. You may still PARK and REJECT, and you must");
            System.out.println("state the staleness in every reply.");
            System.out.println("Delivery Lead: refresh " + STATE_FILE + " (RUNBOOK section 4).");
            System.exit(2);
        }
        if (!WARNS.isEmpty()) {
            System.out.println(y("VERDICT: WARNINGS"));
            WARNS.forEach(w -> System.out.println("  - " + w));
            System.out.println("\nAgents: proceed, and state the warning in replies that depend on it.");
            System.exit(1);
        }
        System.out.println(g("VERDICT: FRESH") + " - governance state is current; run the full pipeline.");
    }

    static Path findRoot() {
        Path p = Paths.get("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve(STATE_FILE)) && !Files.exists(p.resolve(".git"))) {
            p = p.getParent();
        }
        return p == null ? Paths.get("").toAbsolutePath() : p;
    }

    // ------------------------------------------------------- the state file

    @SuppressWarnings("unchecked")
    static Map<String, Object> checkStateFile(LocalDate today) throws IOException {
        System.out.println("State file");
        Path path = root.resolve(STATE_FILE);
        if (!Files.exists(path)) {
            halt(STATE_FILE + " is missing — agents halt triage entirely (Rule CS-1)");
            return null;
        }
        Object parsed;
        try {
            parsed = Yaml.parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (Yaml.YamlException e) {
            // Fail closed: a document we cannot fully parse is a malformed document.
            halt(STATE_FILE + " could not be parsed (line " + e.line + "): " + e.getMessage());
            return null;
        }
        if (!(parsed instanceof Map)) {
            halt(STATE_FILE + " is not a mapping at the top level");
            return null;
        }
        Map<String, Object> state = (Map<String, Object>) parsed;

        LocalDate asOf = date(state.get("state_as_of"));
        if (asOf == null) {
            halt("state_as_of is missing or unparseable — freshness cannot be established (CS-1)");
        } else {
            long age = java.time.temporal.ChronoUnit.DAYS.between(asOf, today);
            if (age > 30)      halt("state_as_of is " + age + " days old (limit 30)");
            else if (age > 14) warn("state_as_of is " + age + " days old — refresh at the next Governance Sync");
            else               pass("state_as_of " + asOf + " (" + age + " days old)");
        }

        LocalDate due = date(state.get("review_due"));
        if (due == null) {
            // CS-1 treats an unestablishable review date as malformed, not as a warning.
            halt("review_due is missing or unparseable — CS-1 requires a halt, not a warning");
        } else if (today.isAfter(due)) {
            halt("review_due passed on " + due);
        } else {
            pass("review_due " + due + " (" + java.time.temporal.ChronoUnit.DAYS.between(today, due) + " days remaining)");
        }

        Object ratified = state.get("ratified_by");
        boolean provisional = Boolean.TRUE.equals(state.get("provisional"));
        if (provisional || ratified == null) {
            warn("state is provisional / unratified - ADMIT restricted to work an authority document already lists");
        } else {
            pass("ratified by " + ratified);
        }
        return state;
    }

    /** Every required path must be present and of the right shape; anything missing is a halt. */
    @SuppressWarnings("unchecked")
    static void checkStructure(Map<String, Object> state) {
        System.out.println("\nState structure");
        for (String key : List.of("schema_version", "workstreams")) {
            if (state.get(key) == null) halt("top-level `" + key + "` is missing");
        }
        Object project = state.get("project");
        if (!(project instanceof Map) || ((Map<String, Object>) project).get("name") == null) {
            halt("`project.name` is missing");
        }
        Object ws = state.get("workstreams");
        if (!(ws instanceof List) || ((List<Object>) ws).isEmpty()) {
            halt("`workstreams` is missing or empty - an agent cannot resolve which lifecycle applies (Rule LC-1)");
            return;
        }
        int i = 0;
        for (Object o : (List<Object>) ws) {
            i++;
            String at = "workstreams[" + i + "]";
            if (!(o instanceof Map)) { halt(at + " is not a mapping"); continue; }
            Map<String, Object> w = (Map<String, Object>) o;
            String id = str(w.get("id"));
            at = id != null ? "workstream " + id : at;

            require(w, "id", at);
            require(w, "name", at);

            Map<String, Object> lc = map(w.get("lifecycle"));
            if (lc == null) halt(at + ": `lifecycle` is missing");
            else {
                require(lc, "current_phase", at + ".lifecycle");
                require(lc, "stage_status", at + ".lifecycle");
            }

            Map<String, Object> obj = map(w.get("current_objective"));
            if (obj == null || obj.get("description") == null) {
                halt(at + ": `current_objective.description` is missing");
            }

            Map<String, Object> sc = map(w.get("current_scope"));
            if (sc == null) halt(at + ": `current_scope` is missing");
            else {
                if (!(sc.get("in_scope") instanceof List))     halt(at + ": `current_scope.in_scope` must be a list");
                if (!(sc.get("out_of_scope") instanceof List)) halt(at + ": `current_scope.out_of_scope` must be a list");
            }

            Map<String, Object> gate = map(w.get("current_gate"));
            if (gate == null) {
                warn(at + ": no `current_gate` - SF0 claims cannot be verified against exit criteria");
            } else {
                if (gate.get("state") == null) halt(at + ": `current_gate.state` is missing");
                Object crit = gate.get("exit_criteria");
                if (!(crit instanceof List) || ((List<Object>) crit).isEmpty()) {
                    halt(at + ": `current_gate.exit_criteria` is missing or empty");
                } else {
                    int n = 0;
                    for (Object c : (List<Object>) crit) {
                        n++;
                        Map<String, Object> cm = map(c);
                        if (cm == null || cm.get("criterion") == null || cm.get("state") == null) {
                            halt(at + ": exit_criteria[" + n + "] needs both `criterion` and `state`");
                        }
                    }
                }
            }
            if (HALTS.isEmpty()) pass(at + ": lifecycle, objective, scope and gate all resolvable");
        }
        if (state.get("standing_constraints") == null) warn("`standing_constraints` is missing - SF4 rejections lose their basis");
        if (state.get("routing") == null)              warn("`routing` is missing - admitted work has no documented home");
    }

    /** Finding B: a sequential counter that is already behind its register mints duplicate IDs. */
    @SuppressWarnings("unchecked")
    static void checkIdAllocation(Map<String, Object> state) throws java.io.UncheckedIOException {
        System.out.println("\nID allocation");
        Map<String, Object> alloc = map(state.get("id_allocation"));
        if (alloc == null) { warn("`id_allocation` is missing - ID minting rules are undefined"); return; }
        Map<String, Object> seq = map(alloc.get("sequential"));
        if (seq == null) { warn("`id_allocation.sequential` is missing"); return; }

        Map<String, Integer> maxUsed = scanMaxIds();
        for (Map.Entry<String, Object> e : seq.entrySet()) {
            String prefix = e.getKey();
            Integer next = intOf(e.getValue());
            if (next == null) { halt("id_allocation.sequential." + prefix + " is not a number"); continue; }
            int used = maxUsed.getOrDefault(prefix, 0);
            if (next <= used) {
                halt("id_allocation.sequential." + prefix + " = " + next + " but " + prefix + "-"
                     + String.format("%03d", used) + " already exists — the next mint would collide");
            } else {
                pass(prefix + ": next " + next + ", highest in use " + (used == 0 ? "none" : prefix + "-" + String.format("%03d", used)));
            }
        }
    }

    static final Pattern ANY_ID = Pattern.compile("\\b([A-Z]{2,6})-([0-9]{3,4})\\b");

    static Map<String, Integer> scanMaxIds() {
        Map<String, Integer> max = new HashMap<>();
        for (Path p : registerFiles()) {
            try {
                Matcher m = ANY_ID.matcher(Files.readString(p, StandardCharsets.UTF_8));
                while (m.find()) {
                    max.merge(m.group(1), Integer.parseInt(m.group(2)), Math::max);
                }
            } catch (IOException ignored) { }
        }
        return max;
    }

    static List<Path> registerFiles() {
        Path dir = root.resolve("docs/governance/registers");
        if (!Files.isDirectory(dir)) return List.of();
        try (var s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".md")).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** An ID is DEFINED where it leads a table row. Two definitions of one ID is a collision. */
    static final Pattern ROW_ID = Pattern.compile("(?m)^\\|\\s*([A-Z]{2,6}-[0-9A-Za-z-]+)\\s*\\|");

    static void checkIdUniqueness() {
        System.out.println("\nID uniqueness");
        Map<String, List<String>> seen = new TreeMap<>();
        for (Path p : registerFiles()) {
            try {
                Matcher m = ROW_ID.matcher(Files.readString(p, StandardCharsets.UTF_8));
                while (m.find()) {
                    seen.computeIfAbsent(m.group(1), k -> new ArrayList<>())
                        .add(root.relativize(p).toString());
                }
            } catch (IOException ignored) { }
        }
        int dupes = 0;
        for (var e : seen.entrySet()) {
            if (e.getValue().size() > 1) {
                halt("ID " + e.getKey() + " is defined " + e.getValue().size() + " times: " + e.getValue());
                dupes++;
            }
        }
        if (dupes == 0) pass(seen.size() + " register IDs, all unique");
    }

    // ------------------------------------------------------- artefact ages

    static void checkArtefactAges(LocalDate today) {
        System.out.println("\nArtefacts");
        for (Artefact a : ARTEFACTS) {
            Path p = root.resolve(a.path());
            if (!Files.exists(p)) {
                if (a.halting()) halt(a.path() + " is missing (owner: " + a.owner() + ")");
                else             warn(a.path() + " is missing (owner: " + a.owner() + ")");
                continue;
            }
            LocalDate touched = lastTouched(a.path(), p);
            long age = java.time.temporal.ChronoUnit.DAYS.between(touched, today);
            String line = String.format("%-58s %4dd  (limit %d, owner: %s)", a.path(), age, a.maxAgeDays(), a.owner());
            if (age > a.maxAgeDays()) {
                if (a.halting()) halt(line);
                else             warn(line);
            } else {
                pass(String.format("%-58s %4dd  (limit %d)", a.path(), age, a.maxAgeDays()));
            }
        }
    }

    static LocalDate lastTouched(String relative, Path absolute) {
        try {
            Process proc = new ProcessBuilder("git", "-C", root.toString(),
                    "log", "-1", "--format=%cI", "--", relative)
                    .redirectErrorStream(false).start();
            String out;
            try (var in = proc.getInputStream()) {
                out = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            proc.waitFor(15, TimeUnit.SECONDS);
            if (!out.isEmpty()) return ZonedDateTime.parse(out).toLocalDate();
        } catch (Exception ignored) { }
        try {
            return Files.getLastModifiedTime(absolute).toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        } catch (IOException e) {
            return LocalDate.MIN;
        }
    }

    // ------------------------------------------------------------- helpers

    static void require(Map<String, Object> m, String key, String at) {
        if (m.get(key) == null) halt(at + ": `" + key + "` is missing");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object o) { return o instanceof Map ? (Map<String, Object>) o : null; }

    static String str(Object o) { return o == null ? null : o.toString(); }

    static Integer intOf(Object o) {
        try { return o == null ? null : Integer.valueOf(o.toString().trim()); }
        catch (NumberFormatException e) { return null; }
    }

    static LocalDate date(Object o) {
        if (o == null) return null;
        try { return LocalDate.parse(o.toString().trim()); }
        catch (DateTimeParseException e) { return null; }
    }

    // ================================================================ YAML
    /**
     * A deliberately small YAML reader for the subset the governance state file uses:
     * block mappings, block sequences, plain and quoted scalars, folded (>) and literal (|)
     * block scalars, flow sequences of scalars, comments, and null.
     *
     * It FAILS CLOSED: any construct outside that subset — anchors, aliases, tags, flow
     * mappings, multiple documents, tabs — raises YamlException, which the caller reports
     * as a halt. Silently accepting a document we do not fully understand is the failure
     * mode this replaces.
     */
    static final class Yaml {

        static final class YamlException extends RuntimeException {
            final int line;
            YamlException(String msg, int line) { super(msg); this.line = line; }
        }

        record Line(int indent, String text, String raw, int no, boolean blank) {}

        private final List<Line> lines = new ArrayList<>();
        private int i = 0;

        static Object parse(String source) {
            Yaml y = new Yaml();
            y.tokenize(source);
            y.skipBlanks();
            if (y.i >= y.lines.size()) return new LinkedHashMap<String, Object>();
            Object v = y.parseNode(y.lines.get(y.i).indent());
            y.skipBlanks();
            if (y.i < y.lines.size()) {
                throw new YamlException("unconsumed content", y.lines.get(y.i).no());
            }
            return v;
        }

        private void tokenize(String source) {
            int no = 0;
            for (String raw : source.split("\n", -1)) {
                no++;
                if (raw.indexOf('\t') >= 0 && !raw.strip().startsWith("#")) {
                    throw new YamlException("tab character - YAML forbids tabs for indentation", no);
                }
                String stripped = stripComment(raw, no);
                if (stripped.isBlank()) {
                    lines.add(new Line(-1, "", raw, no, true));
                    continue;
                }
                int indent = 0;
                while (indent < stripped.length() && stripped.charAt(indent) == ' ') indent++;
                String text = stripped.substring(indent).stripTrailing();
                if (text.equals("---") || text.equals("...")) {
                    throw new YamlException("multi-document YAML is not supported here", no);
                }
                if (text.startsWith("&") || text.startsWith("*") || text.startsWith("!")) {
                    throw new YamlException("anchors, aliases and tags are not supported here", no);
                }
                lines.add(new Line(indent, text, raw, no, false));
            }
        }

        /**
         * Remove an unquoted trailing comment. A '#' inside quotes is content.
         *
         * A quote character only OPENS a quoted scalar at a token boundary, so the
         * apostrophe in `the SSOT's decisions` is prose, not an opening quote.
         */
        private static String stripComment(String raw, int no) {
            boolean single = false, dbl = false;
            for (int k = 0; k < raw.length(); k++) {
                char c = raw.charAt(k);
                if (c == '\'' && !dbl && (single || atTokenStart(raw, k))) single = !single;
                else if (c == '"' && !single && (dbl || atTokenStart(raw, k))) dbl = !dbl;
                else if (c == '#' && !single && !dbl && (k == 0 || Character.isWhitespace(raw.charAt(k - 1)))) {
                    return raw.substring(0, k);
                }
            }
            if (single || dbl) throw new YamlException("unterminated quoted string", no);
            return raw;
        }

        /** True when position k could begin a scalar: line start, or after a structural character. */
        private static boolean atTokenStart(String s, int k) {
            if (k == 0) return true;
            char p = s.charAt(k - 1);
            return p == ' ' || p == '\t' || p == ':' || p == '-' || p == '[' || p == ',' || p == '{';
        }

        private void skipBlanks() { while (i < lines.size() && lines.get(i).blank()) i++; }

        private Line peek() { skipBlanks(); return i < lines.size() ? lines.get(i) : null; }

        private Object parseNode(int indent) {
            Line l = peek();
            if (l == null || l.indent() < indent) return null;
            return isSeqItem(l.text()) ? parseSeq(indent) : parseMap(indent);
        }

        private static boolean isSeqItem(String text) {
            return text.equals("-") || text.startsWith("- ");
        }

        private Map<String, Object> parseMap(int indent) {
            Map<String, Object> m = new LinkedHashMap<>();
            while (true) {
                Line l = peek();
                if (l == null || l.indent() != indent || isSeqItem(l.text())) break;
                int colon = keyColon(l.text());
                if (colon < 0) throw new YamlException("expected `key: value`, got: " + l.text(), l.no());
                String key = unquote(l.text().substring(0, colon).strip());
                String rest = l.text().substring(colon + 1).strip();
                i++;
                if (rest.equals(">") || rest.equals("|") || rest.equals(">-") || rest.equals("|-")) {
                    m.put(key, blockScalar(indent, rest.startsWith(">")));
                } else if (rest.isEmpty()) {
                    Line next = peek();
                    m.put(key, (next != null && next.indent() > indent) ? parseNode(next.indent()) : null);
                } else {
                    m.put(key, scalar(rest, l.no()));
                }
            }
            return m;
        }

        private List<Object> parseSeq(int indent) {
            List<Object> out = new ArrayList<>();
            while (true) {
                Line l = peek();
                if (l == null || l.indent() != indent || !isSeqItem(l.text())) break;
                String after = l.text().substring(1);
                String content = after.strip();
                if (content.isEmpty()) {
                    i++;
                    Line next = peek();
                    out.add((next != null && next.indent() > indent) ? parseNode(next.indent()) : null);
                } else if (keyColon(content) >= 0) {
                    // `- key: value` — the item is a mapping whose first key sits on this line.
                    // Its keys are indented to where that first key starts: past the '-' and
                    // any spaces after it.
                    int lead = 0;
                    while (lead < after.length() && after.charAt(lead) == ' ') lead++;
                    int virtual = indent + 1 + lead;
                    lines.set(i, new Line(virtual, content, l.raw(), l.no(), false));
                    out.add(parseMap(virtual));
                } else {
                    i++;
                    out.add(scalar(content, l.no()));
                }
            }
            return out;
        }

        private String blockScalar(int parentIndent, boolean folded) {
            StringBuilder sb = new StringBuilder();
            while (i < lines.size()) {
                Line l = lines.get(i);
                if (l.blank()) { i++; if (!folded) sb.append('\n'); continue; }
                if (l.indent() <= parentIndent) break;
                if (!sb.isEmpty()) sb.append(folded ? ' ' : '\n');
                sb.append(l.raw().strip());
                i++;
            }
            return sb.toString().strip();
        }

        /** Index of the ':' that separates a key from its value, ignoring quotes and brackets. */
        private static int keyColon(String text) {
            boolean single = false, dbl = false;
            int depth = 0;
            for (int k = 0; k < text.length(); k++) {
                char c = text.charAt(k);
                if (c == '\'' && !dbl) single = !single;
                else if (c == '"' && !single) dbl = !dbl;
                else if (!single && !dbl) {
                    if (c == '[' || c == '{') depth++;
                    else if (c == ']' || c == '}') depth--;
                    else if (c == ':' && depth == 0 && (k + 1 == text.length() || text.charAt(k + 1) == ' ')) {
                        return k;
                    }
                }
            }
            return -1;
        }

        private static Object scalar(String text, int no) {
            String t = text.strip();
            // Anchors, aliases and tags can appear in the VALUE position, not just at the
            // start of a line. An unquoted scalar opening with one of these is a construct
            // this reader does not implement, so it fails closed rather than mis-reading it.
            if (t.startsWith("&") || t.startsWith("*") || t.startsWith("!")) {
                throw new YamlException("anchors, aliases and tags are not supported here", no);
            }
            if (t.startsWith("{")) throw new YamlException("flow mappings are not supported here", no);
            if (t.startsWith("[")) {
                if (!t.endsWith("]")) throw new YamlException("unterminated flow sequence", no);
                List<Object> out = new ArrayList<>();
                String inner = t.substring(1, t.length() - 1).strip();
                if (inner.isEmpty()) return out;
                for (String part : splitFlow(inner, no)) out.add(scalar(part, no));
                return out;
            }
            if (t.isEmpty() || t.equals("null") || t.equals("~")) return null;
            if (t.equals("true")) return Boolean.TRUE;
            if (t.equals("false")) return Boolean.FALSE;
            return unquote(t);
        }

        private static List<String> splitFlow(String inner, int no) {
            List<String> parts = new ArrayList<>();
            boolean single = false, dbl = false;
            int start = 0, depth = 0;
            for (int k = 0; k < inner.length(); k++) {
                char c = inner.charAt(k);
                if (c == '\'' && !dbl) single = !single;
                else if (c == '"' && !single) dbl = !dbl;
                else if (!single && !dbl) {
                    if (c == '[') depth++;
                    else if (c == ']') depth--;
                    else if (c == ',' && depth == 0) { parts.add(inner.substring(start, k)); start = k + 1; }
                }
            }
            if (single || dbl) throw new YamlException("unterminated quoted string in flow sequence", no);
            parts.add(inner.substring(start));
            return parts;
        }

        private static String unquote(String t) {
            if (t.length() >= 2 && ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'")))) {
                return t.substring(1, t.length() - 1);
            }
            return t;
        }
    }
}
