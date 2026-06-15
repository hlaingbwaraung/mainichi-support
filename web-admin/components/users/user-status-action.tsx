"use client";

import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { setUserDisabled } from "@/lib/admin-actions";

export function UserStatusAction({
  userId,
  initiallyDisabled
}: {
  userId: string;
  initiallyDisabled: boolean;
}) {
  const [disabled, setDisabled] = useState(initiallyDisabled);
  const [busy, setBusy] = useState(false);

  async function update() {
    setBusy(true);
    try {
      await setUserDisabled(userId, !disabled);
      setDisabled(!disabled);
      toast.success(disabled ? "利用を再開しました。" : "利用を停止しました。");
    } catch {
      toast.error("状態を変更できませんでした。");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Button
      variant={disabled ? "default" : "destructive"}
      onClick={update}
      disabled={busy}
    >
      {busy ? "変更中..." : disabled ? "利用を再開" : "利用を停止"}
    </Button>
  );
}
