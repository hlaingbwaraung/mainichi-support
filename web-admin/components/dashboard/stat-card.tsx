import type { LucideIcon } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export function StatCard({
  label,
  value,
  hint,
  icon: Icon,
  tone = "teal"
}: {
  label: string;
  value: string;
  hint: string;
  icon: LucideIcon;
  tone?: "teal" | "gold" | "green" | "red";
}) {
  const tones = {
    teal: "bg-cyan-50 text-cyan-800 dark:bg-cyan-950 dark:text-cyan-200",
    gold: "bg-amber-50 text-amber-800 dark:bg-amber-950 dark:text-amber-200",
    green:
      "bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200",
    red: "bg-red-50 text-red-800 dark:bg-red-950 dark:text-red-200"
  };

  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-4 pt-5">
        <div className="min-w-0">
          <p className="text-sm font-medium text-muted-foreground">{label}</p>
          <p className="mt-2 text-3xl font-bold">{value}</p>
          <p className="mt-2 text-xs text-muted-foreground">{hint}</p>
        </div>
        <div className={cn("grid h-11 w-11 shrink-0 place-items-center rounded-md", tones[tone])}>
          <Icon className="h-5 w-5" />
        </div>
      </CardContent>
    </Card>
  );
}
