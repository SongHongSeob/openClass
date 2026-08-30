// @MX:NOTE: [AUTO] AC-FE-109 컴파일 타임 회귀 가드 — sync-auditor F1 발견
// (`.moai/specs/SPEC-FRONTEND-001/progress.md` §H.5) 대응. `WaitlistEntryId`
// 브랜디드 타입이 `cancelWaitlistEntry(item.position, token)` 같은 오배선을
// 타입 검사에서 실제로 거부하는지 증명한다.
//
// 이 파일은 `vite.config.ts`의 `test.include: ['src/**/*.test.ts']`와
// 일치하지 않으므로(파일명이 `.test.ts`로 끝나지 않음) vitest가 실행하지
// 않는다 — 아래 호출은 런타임에 절대 실행되지 않으며, `npx tsc -b --force`의
// 타입 검사 대상으로만 존재한다(`tsconfig.app.json`의 `include: ["src"]`).

import { cancelWaitlistEntry } from './endpoints'
import type { WaitlistListItem } from './types'

declare const item: WaitlistListItem
declare const token: string

// AC-FE-109 / design.md §A.1 판정 기준 — "position을 취소 함수의 인자로
// 넘기는 코드가 타입 검사에서 거부되는가". `WaitlistEntryId`가 `position`
// (순수 number)과 구조적으로 구별되므로 아래 호출은 TS2345로 거부된다.
//
// 회귀 감지 메커니즘: 브랜드가 제거되거나 우회되면 이 호출은 더 이상 타입
// 오류를 내지 않으므로, 아래 `@ts-expect-error`가 "Unused '@ts-expect-error'
// directive"(TS2578)를 내며 `npx tsc -b --force`가 실패한다.
// @ts-expect-error TS2345 — WaitlistEntryId는 position(number)을 구조적으로 거부한다
cancelWaitlistEntry(item.position, token)
