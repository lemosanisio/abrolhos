# Frontend Improvements for Abrolhos

## Overview
This document outlines critical frontend improvements for the Abrolhos application, focusing on error handling, accessibility, user experience, performance, and code quality.

## Current Frontend Issues

### 1. No Error Boundaries
**Location**: React component tree

**Issue**: Unhandled errors in components crash the entire application with white screen.

**Impact**:
- Poor user experience on errors
- No graceful degradation
- Lost user data on crashes
- No error reporting to developers

### 2. Inconsistent Error Handling
**Location**: API calls and form submissions

**Issue**: Error handling varies across components, some errors not displayed to users.

**Impact**:
- Confusing user experience
- Silent failures
- No feedback on failed operations
- Difficult debugging

### 3. Limited Accessibility
**Location**: All components and pages

**Issue**: Missing ARIA labels, poor keyboard navigation, insufficient color contrast.

**Impact**:
- Excludes users with disabilities
- Poor screen reader support
- Fails WCAG 2.1 guidelines
- Legal compliance risks

### 4. No Loading States Management
**Location**: Data fetching hooks

**Issue**: Inconsistent loading indicators, no skeleton screens, poor loading UX.

**Impact**:
- Perceived slow performance
- User confusion during loads
- Multiple clicks on buttons
- Poor mobile experience

### 5. Missing Form Validation Feedback
**Location**: Forms (login, post creation, activation)

**Issue**: Limited validation feedback, errors not clearly displayed.

**Impact**:
- User frustration
- Increased support requests
- Form abandonment
- Data quality issues

---

## Proposed Solutions

### Solution 1: Implement Error Boundaries

#### Requirements
```xml
<requirement id="FE-001" priority="critical">
  <title>Add React Error Boundaries</title>
  <description>
    Implement error boundaries to catch and handle React component errors gracefully
  </description>
  <acceptance-criteria>
    - Root error boundary catches all unhandled errors
    - Page-level error boundaries for isolated failures
    - Error UI shows user-friendly message
    - Error details logged to console in development
    - Error reporting to monitoring service in production
    - Reset functionality to recover from errors
    - Fallback UI maintains navigation structure
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Root Error Boundary**:
```typescript
// src/components/organisms/ErrorBoundary.tsx
import { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
    
    // Report to monitoring service
    if (import.meta.env.PROD) {
      // TODO-USER: Send to Sentry, LogRocket, etc.
    }
    
    this.props.onError?.(error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: undefined });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
          <div className="max-w-md w-full bg-white shadow-lg rounded-lg p-8">
            <h1 className="text-2xl font-bold text-red-600 mb-4">
              Oops! Something went wrong
            </h1>
            <p className="text-gray-600 mb-6">
              We're sorry, but something unexpected happened. Please try refreshing the page.
            </p>
            {import.meta.env.DEV && this.state.error && (
              <pre className="bg-gray-100 p-4 rounded text-xs overflow-auto mb-4">
                {this.state.error.message}
              </pre>
            )}
            <div className="flex gap-4">
              <button
                onClick={this.handleReset}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
              >
                Try Again
              </button>
              <button
                onClick={() => window.location.href = '/'}
                className="px-4 py-2 bg-gray-200 text-gray-800 rounded hover:bg-gray-300"
              >
                Go Home
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
```

**Usage in App**:
```typescript
// src/App.tsx
import { ErrorBoundary } from './components/organisms/ErrorBoundary';

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </ErrorBoundary>
  );
}
```

**Page-Level Error Boundaries**:
```typescript
// Wrap each route with error boundary
<Route
  path="/posts/:slug"
  element={
    <ErrorBoundary fallback={<PostErrorFallback />}>
      <PostDetailPage />
    </ErrorBoundary>
  }
/>
```

#### Testing Strategy
- Unit tests for error boundary component
- Tests for error logging
- Tests for reset functionality
- Integration tests simulating component errors
- Visual regression tests for error UI

---

### Solution 2: Standardize Error Handling

#### Requirements
```xml
<requirement id="FE-002" priority="high">
  <title>Implement Consistent Error Handling</title>
  <description>
    Create standardized error handling utilities and components for consistent error display across the application
  </description>
  <acceptance-criteria>
    - Centralized error handling utility
    - Consistent error message formatting
    - Toast notifications for errors
    - Form field error display
    - Network error detection and retry
    - User-friendly error messages (no technical jargon)
    - Error categorization (network, validation, server, client)
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Error Handling Utility**:
```typescript
// src/core/utils/errorHandler.ts
export enum ErrorType {
  NETWORK = 'network',
  VALIDATION = 'validation',
  SERVER = 'server',
  CLIENT = 'client',
  AUTH = 'auth',
}

export interface AppError {
  type: ErrorType;
  message: string;
  details?: unknown;
  retryable: boolean;
}

export function handleApiError(error: unknown): AppError {
  // Network errors
  if (error instanceof TypeError && error.message.includes('fetch')) {
    return {
      type: ErrorType.NETWORK,
      message: 'Unable to connect to server. Please check your internet connection.',
      retryable: true,
    };
  }

  // API errors
  if (error && typeof error === 'object' && 'status' in error) {
    const status = (error as { status: number }).status;
    
    switch (status) {
      case 400:
        return {
          type: ErrorType.VALIDATION,
          message: 'Invalid request. Please check your input.',
          details: error,
          retryable: false,
        };
      case 401:
        return {
          type: ErrorType.AUTH,
          message: 'Your session has expired. Please log in again.',
          retryable: false,
        };
      case 403:
        return {
          type: ErrorType.AUTH,
          message: 'You do not have permission to perform this action.',
          retryable: false,
        };
      case 404:
        return {
          type: ErrorType.CLIENT,
          message: 'The requested resource was not found.',
          retryable: false,
        };
      case 429:
        return {
          type: ErrorType.SERVER,
          message: 'Too many requests. Please try again later.',
          retryable: true,
        };
      case 500:
      case 502:
      case 503:
        return {
          type: ErrorType.SERVER,
          message: 'Server error. Please try again later.',
          retryable: true,
        };
      default:
        return {
          type: ErrorType.SERVER,
          message: 'An unexpected error occurred.',
          retryable: true,
        };
    }
  }

  // Unknown errors
  return {
    type: ErrorType.CLIENT,
    message: 'An unexpected error occurred.',
    details: error,
    retryable: false,
  };
}
```

**Toast Notification Component**:
```typescript
// src/components/molecules/Toast.tsx
import { useEffect } from 'react';
import { ErrorType } from '../../core/utils/errorHandler';

interface ToastProps {
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  onClose: () => void;
  duration?: number;
}

export function Toast({ type, message, onClose, duration = 5000 }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(onClose, duration);
    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const bgColor = {
    success: 'bg-green-500',
    error: 'bg-red-500',
    warning: 'bg-yellow-500',
    info: 'bg-blue-500',
  }[type];

  return (
    <div className={`${bgColor} text-white px-6 py-4 rounded-lg shadow-lg flex items-center justify-between`}>
      <span>{message}</span>
      <button onClick={onClose} className="ml-4 text-white hover:text-gray-200">
        ✕
      </button>
    </div>
  );
}

// Toast Container
export function ToastContainer({ toasts }: { toasts: ToastProps[] }) {
  return (
    <div className="fixed top-4 right-4 z-50 space-y-2">
      {toasts.map((toast, index) => (
        <Toast key={index} {...toast} />
      ))}
    </div>
  );
}
```

**Error Display Hook**:
```typescript
// src/core/hooks/useErrorHandler.ts
import { useState, useCallback } from 'react';
import { handleApiError, AppError } from '../utils/errorHandler';

export function useErrorHandler() {
  const [error, setError] = useState<AppError | null>(null);

  const handleError = useCallback((err: unknown) => {
    const appError = handleApiError(err);
    setError(appError);
    
    // Show toast notification
    // TODO-USER: Integrate with toast system
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return { error, handleError, clearError };
}
```

#### Testing Strategy
- Unit tests for error handling utility
- Tests for error categorization
- Tests for toast notifications
- Integration tests for API error handling
- Visual tests for error displays

---

### Solution 3: Improve Accessibility

#### Requirements
```xml
<requirement id="FE-003" priority="high">
  <title>Enhance Accessibility Compliance</title>
  <description>
    Improve accessibility to meet WCAG 2.1 Level AA standards
  </description>
  <acceptance-criteria>
    - All interactive elements keyboard accessible
    - ARIA labels on all form inputs
    - Semantic HTML throughout
    - Color contrast ratio &gt;= 4.5:1 for text
    - Focus indicators visible on all interactive elements
    - Screen reader announcements for dynamic content
    - Skip navigation links
    - Alt text for all images
    - Form validation errors announced to screen readers
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Accessible Form Components**:
```typescript
// src/components/atoms/Input.tsx
import { forwardRef, InputHTMLAttributes } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  helperText?: string;
  required?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, required, id, ...props }, ref) => {
    const inputId = id || `input-${label.toLowerCase().replace(/\s/g, '-')}`;
    const errorId = `${inputId}-error`;
    const helperId = `${inputId}-helper`;

    return (
      <div className="mb-4">
        <label
          htmlFor={inputId}
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          {label}
          {required && <span className="text-red-500 ml-1" aria-label="required">*</span>}
        </label>
        <input
          ref={ref}
          id={inputId}
          aria-invalid={!!error}
          aria-describedby={error ? errorId : helperText ? helperId : undefined}
          aria-required={required}
          className={`
            w-full px-3 py-2 border rounded-md
            focus:outline-none focus:ring-2 focus:ring-blue-500
            ${error ? 'border-red-500' : 'border-gray-300'}
          `}
          {...props}
        />
        {error && (
          <p id={errorId} className="mt-1 text-sm text-red-600" role="alert">
            {error}
          </p>
        )}
        {helperText && !error && (
          <p id={helperId} className="mt-1 text-sm text-gray-500">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
```

**Accessible Button Component**:
```typescript
// src/components/atoms/Button.tsx
import { ButtonHTMLAttributes, forwardRef } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
  loading?: boolean;
  loadingText?: string;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ children, variant = 'primary', loading, loadingText, disabled, ...props }, ref) => {
    const baseClasses = 'px-4 py-2 rounded-md font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors';
    
    const variantClasses = {
      primary: 'bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500',
      secondary: 'bg-gray-200 text-gray-800 hover:bg-gray-300 focus:ring-gray-500',
      danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
    };

    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        aria-busy={loading}
        aria-live="polite"
        className={`${baseClasses} ${variantClasses[variant]} ${(disabled || loading) ? 'opacity-50 cursor-not-allowed' : ''}`}
        {...props}
      >
        {loading ? (
          <>
            <span className="inline-block animate-spin mr-2">⏳</span>
            {loadingText || 'Loading...'}
          </>
        ) : (
          children
        )}
      </button>
    );
  }
);

Button.displayName = 'Button';
```

**Skip Navigation**:
```typescript
// src/components/molecules/SkipNav.tsx
export function SkipNav() {
  return (
    <a
      href="#main-content"
      className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-blue-600 focus:text-white focus:rounded"
    >
      Skip to main content
    </a>
  );
}
```

**Screen Reader Announcements**:
```typescript
// src/components/atoms/LiveRegion.tsx
interface LiveRegionProps {
  message: string;
  politeness?: 'polite' | 'assertive';
}

export function LiveRegion({ message, politeness = 'polite' }: LiveRegionProps) {
  return (
    <div
      role="status"
      aria-live={politeness}
      aria-atomic="true"
      className="sr-only"
    >
      {message}
    </div>
  );
}
```

#### Accessibility Testing
- Automated testing with axe-core
- Keyboard navigation testing
- Screen reader testing (NVDA, JAWS, VoiceOver)
- Color contrast validation
- Focus management testing

---

### Solution 4: Improve Loading States

#### Requirements
```xml
<requirement id="FE-004" priority="medium">
  <title>Enhance Loading State Management</title>
  <description>
    Implement consistent loading indicators and skeleton screens for better perceived performance
  </description>
  <acceptance-criteria>
    - Skeleton screens for post lists
    - Skeleton screens for post detail
    - Loading spinners for buttons
    - Progress indicators for long operations
    - Optimistic UI updates where appropriate
    - Loading states prevent duplicate submissions
    - Smooth transitions between loading and loaded states
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Skeleton Components**:
```typescript
// src/components/atoms/Skeleton.tsx
export function Skeleton({ className = '' }: { className?: string }) {
  return (
    <div className={`animate-pulse bg-gray-200 rounded ${className}`} />
  );
}

// src/components/molecules/PostSummarySkeleton.tsx
export function PostSummarySkeleton() {
  return (
    <div className="border rounded-lg p-6 space-y-4">
      <Skeleton className="h-8 w-3/4" />
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-2/3" />
      <div className="flex gap-2">
        <Skeleton className="h-6 w-20" />
        <Skeleton className="h-6 w-20" />
      </div>
    </div>
  );
}

// src/components/organisms/PostListSkeleton.tsx
export function PostListSkeleton({ count = 5 }: { count?: number }) {
  return (
    <div className="space-y-6">
      {Array.from({ length: count }).map((_, i) => (
        <PostSummarySkeleton key={i} />
      ))}
    </div>
  );
}
```

**Loading Hook**:
```typescript
// src/core/hooks/useLoadingState.ts
import { useState, useCallback } from 'react';

export function useLoadingState(initialState = false) {
  const [isLoading, setIsLoading] = useState(initialState);

  const startLoading = useCallback(() => setIsLoading(true), []);
  const stopLoading = useCallback(() => setIsLoading(false), []);

  const withLoading = useCallback(async <T,>(fn: () => Promise<T>): Promise<T> => {
    startLoading();
    try {
      return await fn();
    } finally {
      stopLoading();
    }
  }, [startLoading, stopLoading]);

  return { isLoading, startLoading, stopLoading, withLoading };
}
```

**Usage in Components**:
```typescript
// src/pages/PostsSummariesListPage.tsx
export function PostsSummariesListPage() {
  const { data: posts, isLoading, error } = usePostsSummary();

  if (isLoading) {
    return <PostListSkeleton />;
  }

  if (error) {
    return <ErrorDisplay error={error} />;
  }

  return <PostList posts={posts} />;
}
```

#### Testing Strategy
- Visual regression tests for skeletons
- Tests for loading state transitions
- Tests preventing duplicate submissions
- Performance tests for skeleton rendering

---

### Solution 5: Enhanced Form Validation

#### Requirements
```xml
<requirement id="FE-005" priority="medium">
  <title>Improve Form Validation and Feedback</title>
  <description>
    Enhance form validation with Zod schemas and better user feedback
  </description>
  <acceptance-criteria>
    - All forms use Zod validation schemas
    - Real-time validation feedback
    - Clear error messages for each field
    - Success feedback on submission
    - Prevent submission with invalid data
    - Preserve form data on errors
    - Validation errors accessible to screen readers
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Validation Schemas**:
```typescript
// src/core/validation/schemas.ts
import { z } from 'zod';

export const loginSchema = z.object({
  username: z.string()
    .min(3, 'Username must be at least 3 characters')
    .max(20, 'Username must be at most 20 characters')
    .regex(/^[a-zA-Z0-9_]+$/, 'Username can only contain letters, numbers, and underscores'),
  totpCode: z.string()
    .length(6, 'TOTP code must be 6 digits')
    .regex(/^\d{6}$/, 'TOTP code must contain only digits'),
});

export const postSchema = z.object({
  title: z.string()
    .min(1, 'Title is required')
    .max(200, 'Title must be at most 200 characters'),
  slug: z.string()
    .min(1, 'Slug is required')
    .max(200, 'Slug must be at most 200 characters')
    .regex(/^[a-z0-9-]+$/, 'Slug can only contain lowercase letters, numbers, and hyphens'),
  content: z.string()
    .min(1, 'Content is required'),
  status: z.enum(['DRAFT', 'PUBLISHED', 'SCHEDULED', 'ARCHIVED']),
  categoryIds: z.array(z.string().uuid()).min(1, 'At least one category is required'),
  tagIds: z.array(z.string().uuid()),
});
```

**Form Hook with Validation**:
```typescript
// src/core/hooks/useValidatedForm.ts
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

export function useValidatedForm<T extends z.ZodType>(schema: T) {
  return useForm<z.infer<T>>({
    resolver: zodResolver(schema),
    mode: 'onBlur', // Validate on blur for better UX
  });
}
```

**Usage in Forms**:
```typescript
// src/components/organisms/LoginForm.tsx
import { useValidatedForm } from '../../core/hooks/useValidatedForm';
import { loginSchema } from '../../core/validation/schemas';

export function LoginForm() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = 
    useValidatedForm(loginSchema);

  const onSubmit = async (data: z.infer<typeof loginSchema>) => {
    try {
      await login(data);
    } catch (error) {
      // Handle error
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input
        label="Username"
        {...register('username')}
        error={errors.username?.message}
        required
      />
      <Input
        label="TOTP Code"
        {...register('totpCode')}
        error={errors.totpCode?.message}
        required
      />
      <Button type="submit" loading={isSubmitting}>
        Log In
      </Button>
    </form>
  );
}
```

#### Testing Strategy
- Unit tests for validation schemas
- Integration tests for form submission
- Tests for error display
- Tests for accessibility of errors
- Visual regression tests for forms

---

## Additional Frontend Improvements

### 6. Performance Optimization

#### Requirements
```xml
<requirement id="FE-006" priority="medium">
  <title>Optimize Frontend Performance</title>
  <description>
    Implement code splitting, lazy loading, and other performance optimizations
  </description>
  <acceptance-criteria>
    - Route-based code splitting
    - Lazy loading for heavy components (editor)
    - Image optimization and lazy loading
    - Bundle size &lt; 200KB (gzipped)
    - First contentful paint &lt; 1.5s
    - Time to interactive &lt; 3s
    - Lighthouse score &gt; 90
  </acceptance-criteria>
</requirement>
```

#### Implementation
- Use React.lazy() for route components
- Implement intersection observer for image lazy loading
- Optimize bundle with tree shaking
- Use dynamic imports for heavy libraries (React Quill)

---

### 7. State Management Improvements

#### Requirements
```xml
<requirement id="FE-007" priority="low">
  <title>Enhance State Management</title>
  <description>
    Improve state management with better patterns and tools
  </description>
  <acceptance-criteria>
    - Centralized API state with React Query
    - Optimistic updates for mutations
    - Automatic cache invalidation
    - Retry logic for failed requests
    - Offline support with cache
  </acceptance-criteria>
</requirement>
```

---

## Testing Strategy

### Unit Tests
- Component rendering tests
- Hook behavior tests
- Utility function tests
- Validation schema tests

### Integration Tests
- Form submission flows
- Authentication flows
- API integration tests
- Error handling tests

### E2E Tests
- User login flow
- Post creation flow
- Post viewing flow
- Error scenarios

### Accessibility Tests
- Automated axe-core tests
- Keyboard navigation tests
- Screen reader tests
- Color contrast tests

---

## Implementation Roadmap

### Phase 1: Critical (Week 1)
1. Error boundaries (2 days)
2. Standardized error handling (2 days)
3. Basic accessibility fixes (1 day)

### Phase 2: High Priority (Week 2)
1. Loading states and skeletons (2 days)
2. Enhanced form validation (2 days)
3. Accessibility improvements (1 day)

### Phase 3: Medium Priority (Week 3)
1. Performance optimizations (2 days)
2. State management improvements (2 days)
3. Testing and documentation (1 day)

---

## Success Metrics

### User Experience
- Error recovery rate > 90%
- Form completion rate > 80%
- User satisfaction score > 4.5/5

### Accessibility
- WCAG 2.1 Level AA compliance
- Keyboard navigation 100% functional
- Screen reader compatibility verified

### Performance
- Lighthouse score > 90
- First contentful paint < 1.5s
- Time to interactive < 3s
- Bundle size < 200KB gzipped

---

## Dependencies

```json
{
  "dependencies": {
    "@hookform/resolvers": "^3.3.4",
    "react-hook-form": "^7.50.1",
    "zod": "^3.22.4"
  },
  "devDependencies": {
    "@axe-core/react": "^4.8.4",
    "@testing-library/jest-dom": "^6.2.0",
    "@testing-library/react": "^14.1.2",
    "@testing-library/user-event": "^14.5.2"
  }
}
```
