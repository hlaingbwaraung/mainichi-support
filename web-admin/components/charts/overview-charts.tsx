"use client";

import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";

import type { DailyMetric, FeatureUsage } from "@/lib/types";

const tooltipStyle = {
  borderRadius: 8,
  border: "1px solid hsl(var(--border))",
  background: "hsl(var(--card))",
  color: "hsl(var(--card-foreground))"
};

export function UserGrowthChart({ data }: { data: DailyMetric[] }) {
  return (
    <div className="h-[310px] w-full">
      <ResponsiveContainer
        width="100%"
        height="100%"
        minWidth={0}
        initialDimension={{ width: 320, height: 310 }}
      >
        <AreaChart data={data} margin={{ left: -20, right: 8, top: 10 }}>
          <defs>
            <linearGradient id="usersFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#2f7779" stopOpacity={0.35} />
              <stop offset="95%" stopColor="#2f7779" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" vertical={false} opacity={0.25} />
          <XAxis dataKey="date" tickLine={false} axisLine={false} fontSize={12} />
          <YAxis tickLine={false} axisLine={false} fontSize={12} allowDecimals={false} />
          <Tooltip contentStyle={tooltipStyle} />
          <Legend />
          <Area
            name="総ユーザー"
            type="monotone"
            dataKey="users"
            stroke="#2f7779"
            fill="url(#usersFill)"
            strokeWidth={2}
          />
          <Area
            name="7日アクティブ"
            type="monotone"
            dataKey="activeUsers"
            stroke="#b08939"
            fill="transparent"
            strokeWidth={2}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

export function FeatureUsageChart({ data }: { data: FeatureUsage[] }) {
  return (
    <div className="h-[310px] w-full">
      <ResponsiveContainer
        width="100%"
        height="100%"
        minWidth={0}
        initialDimension={{ width: 320, height: 310 }}
      >
        <BarChart
          data={data.slice(0, 7)}
          layout="vertical"
          margin={{ left: 12, right: 16, top: 8, bottom: 8 }}
        >
          <CartesianGrid strokeDasharray="3 3" horizontal={false} opacity={0.25} />
          <XAxis type="number" hide />
          <YAxis
            dataKey="label"
            type="category"
            width={92}
            axisLine={false}
            tickLine={false}
            fontSize={12}
          />
          <Tooltip contentStyle={tooltipStyle} />
          <Bar dataKey="total" name="利用回数" fill="#365f65" radius={[0, 4, 4, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function PlanMixChart({
  premium,
  free
}: {
  premium: number;
  free: number;
}) {
  const data = [
    { name: "プレミアム", value: premium, color: "#b08939" },
    { name: "無料", value: free, color: "#2f7779" }
  ];
  return (
    <div className="h-[230px] w-full">
      <ResponsiveContainer
        width="100%"
        height="100%"
        minWidth={0}
        initialDimension={{ width: 280, height: 230 }}
      >
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            innerRadius={55}
            outerRadius={82}
            paddingAngle={3}
          >
            {data.map((item) => (
              <Cell key={item.name} fill={item.color} />
            ))}
          </Pie>
          <Tooltip contentStyle={tooltipStyle} />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
