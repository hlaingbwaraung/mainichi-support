"use client";

import { httpsCallable } from "firebase/functions";

import { functions, isMockClient } from "@/lib/firebase/client";
import type { AdminRole, AppSettings } from "@/lib/types";

async function callFunction<TInput, TOutput>(name: string, input: TInput) {
  if (isMockClient || !functions) {
    await new Promise((resolve) => setTimeout(resolve, 350));
    return { ok: true } as TOutput;
  }
  const callable = httpsCallable<TInput, TOutput>(functions, name);
  const result = await callable(input);
  return result.data;
}

export function setUserDisabled(userId: string, disabled: boolean) {
  return callFunction<{ userId: string; disabled: boolean }, { ok: boolean }>(
    "setUserDisabled",
    { userId, disabled }
  );
}

export function saveAppSettings(settings: AppSettings) {
  return callFunction<AppSettings, { ok: boolean }>("updateAppSettings", settings);
}

export function upsertAdmin(identifier: string, role: AdminRole) {
  return callFunction<
    { identifier: string; role: AdminRole },
    { ok: boolean; uid?: string }
  >("upsertAdminRole", { identifier, role });
}

export function removeAdmin(uid: string) {
  return callFunction<{ uid: string }, { ok: boolean }>("removeAdmin", { uid });
}

export async function exportUsersCsv() {
  if (isMockClient || !functions) return null;
  const result = await callFunction<Record<string, never>, { csv: string }>(
    "exportUsersCsv",
    {}
  );
  return result.csv;
}
