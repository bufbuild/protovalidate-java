def pct(a; b):
  if a == null or b == null or b == 0 then "~"
  else (((a - b) / b * 100) * 10 | round / 10) as $d
    | if $d > 0 then "+\($d)%" elif $d == 0 then "~" else "\($d)%" end
  end;
def num(x):
  if x == null then "-"
  else (x * 100 | round / 100 | tostring)
  end;

def extract: map({
  key: (.benchmark | split(".") | last),
  time: .primaryMetric.score,
  time_unit: .primaryMetric.scoreUnit,
  alloc: (.secondaryMetrics["·gc.alloc.rate.norm"].score // null)
});

(.[0] | extract) as $b
| (.[1] | extract) as $a
| (["benchmark", "metric", "before", "after", "delta"] | @tsv),
  ($b[] | . as $bi
        | ($a[] | select(.key == $bi.key)) as $ai
        | ([$bi.key, "time",  "\(num($bi.time)) \($bi.time_unit)", "\(num($ai.time)) \($ai.time_unit)", pct($ai.time; $bi.time)] | @tsv),
          ([$bi.key, "alloc", "\(num($bi.alloc)) B/op", "\(num($ai.alloc)) B/op", pct($ai.alloc; $bi.alloc)] | @tsv))
