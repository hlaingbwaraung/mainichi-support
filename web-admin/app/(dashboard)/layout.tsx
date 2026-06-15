import { AppShell } from "@/components/app-shell";
import { requireAdmin } from "@/lib/auth";

export default async function DashboardLayout({
  children
}: {
  children: React.ReactNode;
}) {
  const admin = await requireAdmin();
  return <AppShell admin={admin}>{children}</AppShell>;
}
