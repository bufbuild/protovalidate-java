# this script builds a comparison between runs that used the CEL interpreter for
# protovalidate rule evaluation vs runs that used native Java code for protovalidate
# rule evaluation. The differentiator is the value of the "native" field (true for
# native, false for CEL) and this script groups rows that have the same benchmark
# and metric name, but different values for native.
def pct(a; b):
  if a == null or b == null or b == 0 then "~"
  else (((a - b) / b * 100) * 10 | round / 10) as $d
    | if $d > 0 then "+\($d)%" elif $d == 0 then "~" else "\($d)%" end
  end;
def num(x):
  if x == null then "-"
  else (x * 100 | round / 100 | tostring)
  end;

def row: {
  key: (.benchmark | split(".") | last),
  native: .params.enableNativeRules,
  time: .primaryMetric.score,
  unit: .primaryMetric.scoreUnit,
  alloc: (.secondaryMetrics["·gc.alloc.rate.norm"].score // null)
};

map(row)
| group_by(.key)
| (["benchmark", "metric", "cel", "native", "delta"] | @tsv),
  (.[]
    | (map(select(.native == "false"))[0]) as $b
    | (map(select(.native == "true"))[0])  as $a
    | select($b and $a)
    | ([$b.key, "time",  "\(num($b.time)) \($b.unit)",  "\(num($a.time)) \($a.unit)",  pct($a.time;  $b.time)]  | @tsv),
      ([$b.key, "alloc", "\(num($b.alloc)) B/op",       "\(num($a.alloc)) B/op",       pct($a.alloc; $b.alloc)] | @tsv))