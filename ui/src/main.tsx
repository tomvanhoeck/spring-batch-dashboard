import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { SWRConfig } from 'swr'
import App from './App.tsx'
import './index.css'
import { ThemeProvider } from './context/ThemeContext.tsx'
import httpClient from './api/httpClient.ts'
import { applicationPath, initializeApplicationPath } from './runtime/applicationPath.ts'

/**
 * Default fetcher function for SWR that works with our httpClient
 * Using explicit type definition to avoid TS errors
 */
const defaultFetcher = <T,>(url: string): Promise<T> => {
  return httpClient.get<T>(url);
};

const startApplication = async (): Promise<void> => {
  const runtimeConfigUrl = new URL('./runtime-config.js', document.baseURI).href;
  await import(/* @vite-ignore */ runtimeConfigUrl);
  initializeApplicationPath();

  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <BrowserRouter basename={applicationPath}>
        <SWRConfig
          value={{
            fetcher: defaultFetcher,
            revalidateOnFocus: true,  // Enable revalidation when window regains focus
            revalidateIfStale: true,  // Revalidate if data is stale
            focusThrottleInterval: 1000  // Throttle focus revalidation to 1 second
          }}
        >
          <ThemeProvider>
            <App />
          </ThemeProvider>
        </SWRConfig>
      </BrowserRouter>
    </React.StrictMode>,
  );
};

void startApplication();
