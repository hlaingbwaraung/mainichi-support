import { PageHeader } from "@/components/page-header";
import { SettingsForm } from "@/components/settings/settings-form";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle
} from "@/components/ui/card";
import { requireAdmin } from "@/lib/auth";
import { getAppSettings } from "@/lib/data";
import { formatDateTime } from "@/lib/utils";

export const metadata = { title: "アプリ設定" };
export const dynamic = "force-dynamic";

export default async function SettingsPage() {
  const [settings, admin] = await Promise.all([getAppSettings(), requireAdmin()]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="アプリ設定"
        description="Androidアプリ全体へ配信する運営設定です。"
      />
      <Card>
        <CardHeader>
          <CardTitle>配信設定</CardTitle>
          <CardDescription>
            最終更新: {formatDateTime(settings.updatedAt)}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <SettingsForm initialSettings={settings} role={admin.role} />
        </CardContent>
      </Card>
    </div>
  );
}
