-- database: presto; groups: window;
SELECT nationkey, min(nationkey) OVER (PARTITION BY regionkey ORDER BY comment RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS min FROM tpch.tiny.nation
