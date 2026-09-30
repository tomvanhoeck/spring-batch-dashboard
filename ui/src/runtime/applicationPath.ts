declare global {
  interface Window {
    __SPRING_BATCH_DASHBOARD_CONTEXT_PATH__?: string
  }
}

export let applicationPath = '';

export const initializeApplicationPath = (): void => {
  applicationPath = window.__SPRING_BATCH_DASHBOARD_CONTEXT_PATH__ ?? '';
};

export const applicationUrl = (path: string): string =>
  `${applicationPath}${path.startsWith('/') ? path : `/${path}`}`;
