"use client";

import { AlertTriangle, Save } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { saveAppSettings } from "@/lib/admin-actions";
import type { AdminRole, AppSettings } from "@/lib/types";

export function SettingsForm({
  initialSettings,
  role
}: {
  initialSettings: AppSettings;
  role: AdminRole;
}) {
  const [settings, setSettings] = useState(initialSettings);
  const [busy, setBusy] = useState(false);
  const readOnly = role === "viewer";

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      await saveAppSettings(settings);
      toast.success("アプリ設定を保存しました。");
    } catch {
      toast.error("設定を保存できませんでした。");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-6">
      <section className="border-b pb-6">
        <div className="flex items-start justify-between gap-5">
          <div>
            <h2 className="font-bold">メンテナンスモード</h2>
            <p className="mt-1 max-w-2xl text-sm leading-6 text-muted-foreground">
              有効にすると、Androidアプリへメンテナンス中の案内を表示します。
            </p>
          </div>
          <Switch
            aria-label="メンテナンスモード"
            checked={settings.maintenanceMode}
            onCheckedChange={(checked) =>
              setSettings((current) => ({
                ...current,
                maintenanceMode: checked
              }))
            }
            disabled={readOnly}
          />
        </div>
        {settings.maintenanceMode ? (
          <div className="mt-4 flex gap-3 rounded-md border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-100">
            <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" />
            保存すると利用者のアプリにメンテナンス案内が表示されます。
          </div>
        ) : null}
      </section>

      <section className="grid gap-5 border-b pb-6 lg:grid-cols-2">
        <div className="space-y-2">
          <label htmlFor="minimum-version" className="text-sm font-bold">
            最低対応バージョン
          </label>
          <Input
            id="minimum-version"
            value={settings.minimumAppVersion}
            onChange={(event) =>
              setSettings((current) => ({
                ...current,
                minimumAppVersion: event.target.value
              }))
            }
            placeholder="2.1.0"
            disabled={readOnly}
          />
          <p className="text-xs text-muted-foreground">
            このバージョンより古いアプリを更新対象にします。
          </p>
        </div>
        <div className="flex items-start justify-between gap-5 rounded-md border p-4">
          <div>
            <p className="text-sm font-bold">強制アップデート</p>
            <p className="mt-1 text-xs leading-5 text-muted-foreground">
              更新するまでアプリの通常画面へ進めないようにします。
            </p>
          </div>
          <Switch
            aria-label="強制アップデート"
            checked={settings.forceUpdate}
            onCheckedChange={(checked) =>
              setSettings((current) => ({ ...current, forceUpdate: checked }))
            }
            disabled={readOnly}
          />
        </div>
      </section>

      <section className="space-y-2">
        <label htmlFor="announcement" className="text-sm font-bold">
          お知らせメッセージ
        </label>
        <textarea
          id="announcement"
          value={settings.announcementMessage}
          onChange={(event) =>
            setSettings((current) => ({
              ...current,
              announcementMessage: event.target.value
            }))
          }
          className="min-h-36 w-full resize-y rounded-md border bg-background p-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          maxLength={240}
          disabled={readOnly}
          placeholder="利用者に知らせたい内容を入力"
        />
        <p className="text-right text-xs text-muted-foreground">
          {settings.announcementMessage.length}/240
        </p>
      </section>

      <div className="flex justify-end">
        {readOnly ? (
          <p className="text-sm text-muted-foreground">
            閲覧者は設定を変更できません。
          </p>
        ) : (
          <Button type="submit" size="lg" disabled={busy}>
            <Save className="h-5 w-5" />
            {busy ? "保存中..." : "設定を保存"}
          </Button>
        )}
      </div>
    </form>
  );
}
