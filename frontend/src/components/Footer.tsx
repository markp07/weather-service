import React from "react";

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="mt-8 py-4 border-t border-gray-200 dark:border-gray-700 text-center text-xs text-gray-500 dark:text-gray-400">
      <p>
        <a
          href="https://github.com/markp07/weather-service/blob/main/LICENSE"
          target="_blank"
          rel="noopener noreferrer"
          className="underline hover:text-gray-700 dark:hover:text-gray-200"
        >
          MIT License
        </a>{" "}
        {currentYear}{" "}
        <a
          href="https://www.markpost.nl"
          target="_blank"
          rel="noopener noreferrer"
          className="underline hover:text-gray-700 dark:hover:text-gray-200"
        >
          Mark Post - MarkPost.nl
        </a>
      </p>
      <p className="mt-1">
        Data from{" "}
        <a
          href="https://open-meteo.com"
          target="_blank"
          rel="noopener noreferrer"
          className="underline hover:text-gray-700 dark:hover:text-gray-200"
        >
          Open-Meteo
        </a>
        {" | "}
        <a
          href="https://www.bigdatacloud.com"
          target="_blank"
          rel="noopener noreferrer"
          className="underline hover:text-gray-700 dark:hover:text-gray-200"
        >
          BigDataCloud
        </a>
      </p>
    </footer>
  );
}
