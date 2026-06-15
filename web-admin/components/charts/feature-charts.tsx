"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";

import type { FeatureDailyMetric, FeatureUsage } from "@/lib/types";

const colors = ["#2f7779", "#b08939", "#4f6b43", "#8b4b43", "#6f5b87"];

export function FeatureRankingChart({ data }: { data: FeatureUsage[] }) {
  return (
    <div className="h-[360px]">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ left: -10, right: 12 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} opacity={0.25} />
          <XAxis dataKey="label" axisLine={false} tickLine={false} fontSize={12} />
          <YAxis axisLine={false} tickLine={false} fontSize={12} />
          <Tooltip />
          <Bar dataKey="total" name="利用回数" fill="#2f7779" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function FeatureTrendChart({ data }: { data: FeatureDailyMetric[] }) {
  const topFeatures = Array.from(new Set(data.map((item) => item.feature))).slice(0, 5);
  const dates = Array.from(new Set(data.map((item) => item.date)));
  const chartData = dates.map((date) => {
    const row: Record<string, string | number> = { date };
    for (const feature of topFeatures) {
      const item = data.find(
        (candidate) => candidate.date === date && candidate.feature === feature
      );
      row[feature] = item?.count || 0;
      if (item) row[`${feature}Label`] = item.label;
    }
    return row;
  });

  return (
    <div className="h-[360px]">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ left: -12, right: 12 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} opacity={0.25} />
          <XAxis dataKey="date" axisLine={false} tickLine={false} fontSize={12} />
          <YAxis axisLine={false} tickLine={false} fontSize={12} />
          <Tooltip />
          <Legend />
          {topFeatures.map((feature, index) => {
            const label = data.find((item) => item.feature === feature)?.label || feature;
            return (
              <Line
                key={feature}
                type="monotone"
                dataKey={feature}
                name={label}
                stroke={colors[index]}
                strokeWidth={2}
                dot={false}
              />
            );
          })}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
