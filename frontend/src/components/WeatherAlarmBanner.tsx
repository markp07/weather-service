import React from "react";
import { useTranslations } from "next-intl";
import { ExclamationTriangleFill } from "react-bootstrap-icons";

interface WeatherAlarmBannerProps {
  alarm: string;
}

const alarmConfig: Record<string, { bg: string; text: string; border: string }> = {
  YELLOW: {
    bg: "bg-yellow-50 dark:bg-yellow-900/20",
    text: "text-yellow-800 dark:text-yellow-300",
    border: "border-yellow-400 dark:border-yellow-600",
  },
  ORANGE: {
    bg: "bg-orange-50 dark:bg-orange-900/20",
    text: "text-orange-800 dark:text-orange-300",
    border: "border-orange-400 dark:border-orange-600",
  },
  RED: {
    bg: "bg-red-50 dark:bg-red-900/20",
    text: "text-red-800 dark:text-red-300",
    border: "border-red-400 dark:border-red-600",
  },
};

export default function WeatherAlarmBanner({ alarm }: WeatherAlarmBannerProps) {
  const tAlarm = useTranslations("alarm");

  if (!alarm || alarm === "GREEN") return null;

  const config = alarmConfig[alarm];
  if (!config) return null;

  const labelKey = alarm.toLowerCase() as "yellow" | "orange" | "red";

  return (
    <div
      className={`flex items-center gap-2 px-4 py-2 rounded-lg border ${config.bg} ${config.text} ${config.border}`}
      role="alert"
    >
      <ExclamationTriangleFill size={18} className="flex-shrink-0" />
      <span className="text-sm font-semibold">{tAlarm(labelKey)}</span>
    </div>
  );
}
