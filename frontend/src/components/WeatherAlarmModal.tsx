import React from "react";
import { useTranslations } from "next-intl";
import { ExclamationTriangleFill, GeoAlt, Clock, Building, InfoCircle } from "react-bootstrap-icons";
import type { AlarmWarning } from "../types/AlarmWarning";

interface WeatherAlarmModalProps {
  open: boolean;
  onClose: () => void;
  alarm: string;
  alarmWarnings: AlarmWarning[];
}

const alarmConfig: Record<string, { bg: string; text: string; border: string; badge: string }> = {
  YELLOW: {
    bg: "bg-yellow-50 dark:bg-yellow-900/20",
    text: "text-yellow-800 dark:text-yellow-300",
    border: "border-yellow-400 dark:border-yellow-600",
    badge: "bg-yellow-400 text-yellow-900",
  },
  ORANGE: {
    bg: "bg-orange-50 dark:bg-orange-900/20",
    text: "text-orange-800 dark:text-orange-300",
    border: "border-orange-400 dark:border-orange-600",
    badge: "bg-orange-500 text-white",
  },
  RED: {
    bg: "bg-red-50 dark:bg-red-900/20",
    text: "text-red-800 dark:text-red-300",
    border: "border-red-400 dark:border-red-600",
    badge: "bg-red-600 text-white",
  },
};

function getWarningAlarmLevel(awarenessLevel: string): string {
  const lower = awarenessLevel.toLowerCase();
  if (lower.startsWith("4") || lower.includes("red") || lower.includes("extreme")) return "RED";
  if (lower.startsWith("3") || lower.includes("orange") || lower.includes("severe")) return "ORANGE";
  if (lower.startsWith("2") || lower.includes("yellow") || lower.includes("moderate")) return "YELLOW";
  return "YELLOW";
}

function formatDateTime(iso?: string): string {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString([], {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

export default function WeatherAlarmModal({ open, onClose, alarm, alarmWarnings }: WeatherAlarmModalProps) {
  const tAlarm = useTranslations("alarm");
  const tModal = useTranslations("alarmModal");

  if (!open) return null;

  const config = alarmConfig[alarm] ?? alarmConfig.YELLOW;

  return (
    <div className="fixed inset-0 z-50 flex items-start sm:items-center justify-center bg-black/50 backdrop-blur-sm p-2 sm:p-4 overflow-y-auto">
      <div className="bg-white dark:bg-gray-900 rounded-xl shadow-2xl w-full max-w-lg relative mx-2 my-auto">
        {/* Header */}
        <div className={`flex items-center gap-3 px-5 py-4 rounded-t-xl border-b ${config.bg} ${config.text} ${config.border}`}>
          <ExclamationTriangleFill size={22} className="flex-shrink-0" />
          <h2 className="text-lg font-bold flex-1">
            {tAlarm(alarm.toLowerCase() as "yellow" | "orange" | "red")}
          </h2>
          <button
            onClick={onClose}
            className="text-current opacity-60 hover:opacity-100 text-2xl font-bold leading-none focus:outline-none"
            aria-label={tModal("close")}
          >
            ×
          </button>
        </div>

        {/* Warning list */}
        <div className="p-5 space-y-4 max-h-[70vh] overflow-y-auto">
          {alarmWarnings.length === 0 ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">{tModal("noDetails")}</p>
          ) : (
            alarmWarnings.map((w, i) => {
              const level = getWarningAlarmLevel(w.awarenessLevel);
              const cfg = alarmConfig[level] ?? alarmConfig.YELLOW;
              // Prefer structured 'event' field; fall back to 'awarenessType'
              const eventLabel = w.event ?? w.awarenessType;
              return (
                <div key={i} className={`rounded-lg border p-4 space-y-2 ${cfg.border} ${cfg.bg}`}>
                  {/* Type + level badge */}
                  <div className="flex items-center gap-2 flex-wrap">
                    {eventLabel && (
                      <span className="font-semibold text-base text-gray-900 dark:text-white">
                        {eventLabel}
                      </span>
                    )}
                    <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${cfg.badge}`}>
                      {tAlarm(level.toLowerCase() as "yellow" | "orange" | "red").split(":")[0]}
                    </span>
                    {/* Severity / certainty / urgency chips */}
                    {w.severity && (
                      <span className="text-xs px-2 py-0.5 rounded-full bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
                        {tModal("severity")}: {w.severity}
                      </span>
                    )}
                    {w.certainty && (
                      <span className="text-xs px-2 py-0.5 rounded-full bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
                        {tModal("certainty")}: {w.certainty}
                      </span>
                    )}
                    {w.urgency && (
                      <span className="text-xs px-2 py-0.5 rounded-full bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300">
                        {tModal("urgency")}: {w.urgency}
                      </span>
                    )}
                  </div>

                  {/* Headline */}
                  {w.headline && (
                    <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{w.headline}</p>
                  )}

                  {/* Description */}
                  {w.description && (
                    <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">{w.description}</p>
                  )}

                  {/* Instruction (recommended public action) */}
                  {w.instruction && (
                    <div className="flex items-start gap-1.5 text-sm text-gray-700 dark:text-gray-300">
                      <InfoCircle size={14} className="flex-shrink-0 mt-0.5" />
                      <span>{w.instruction}</span>
                    </div>
                  )}

                  {/* Area */}
                  {w.areaDesc && (
                    <div className="flex items-start gap-1.5 text-xs text-gray-600 dark:text-gray-400">
                      <GeoAlt size={13} className="flex-shrink-0 mt-0.5" />
                      <span>{w.areaDesc}</span>
                    </div>
                  )}

                  {/* Time range */}
                  {(w.onset || w.expires) && (
                    <div className="flex items-start gap-1.5 text-xs text-gray-600 dark:text-gray-400">
                      <Clock size={13} className="flex-shrink-0 mt-0.5" />
                      <span>
                        {formatDateTime(w.onset)} – {formatDateTime(w.expires)}
                      </span>
                    </div>
                  )}

                  {/* Sender */}
                  {w.senderName && (
                    <div className="flex items-start gap-1.5 text-xs text-gray-500 dark:text-gray-400">
                      <Building size={13} className="flex-shrink-0 mt-0.5" />
                      <span>{tModal("issuedBy")}: {w.senderName}</span>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t border-gray-200 dark:border-gray-700 text-xs text-gray-500 dark:text-gray-400 rounded-b-xl">
          {tModal("source")}
        </div>
      </div>
    </div>
  );
}
