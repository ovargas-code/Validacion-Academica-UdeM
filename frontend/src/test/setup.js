import '@testing-library/jest-dom';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Explicit cleanup — @testing-library/react auto-cleanup doesn't always fire in Vitest 3.2
afterEach(() => cleanup());
