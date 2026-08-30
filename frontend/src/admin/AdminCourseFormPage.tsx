// @MX:NOTE: [AUTO] 강좌 생성·수정 폼(REQ-ADM-004·005·006·007). `courseId`가
// 있으면 수정 모드 — 로드된 강좌 전체 값으로 프리필하고, `PATCH`이지만 매번
// 전 필드를 다시 제출한다(REQ-ADM-005, plan.md AP-8). 정원 증설 안내와
// 409(`CAPACITY_BELOW_ENROLLMENT`) 정원 필드 강조는 `adminModel.ts`의 순수
// 함수(`isCapacityIncrease`·`classifyCourseFormError`)가 판정한다.

import { useEffect, useState, type FormEvent } from 'react'
import { createCourse, getCourseDetail, updateCourse, type CourseFormPayload } from '../api/endpoints'
import { ApiError } from '../api/client'
import type { Course } from '../api/types'
import { classifyCourseFormError, isCapacityIncrease, toFormValues } from './adminModel'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Alert } from '@/components/ui/alert'

export interface AdminCourseFormPageProps {
  token: string
  /** 없으면 생성 모드(REQ-ADM-004), 있으면 수정 모드(REQ-ADM-005)다. */
  courseId?: number
  onSaved: () => void
  onCancel: () => void
}

const BLANK_VALUES: CourseFormPayload = { title: '', description: '', capacity: 1, startsAt: '', endsAt: '' }

type LoadState = { status: 'loading' } | { status: 'ready' } | { status: 'load-error'; message: string }
type SubmitState =
  | { status: 'idle' }
  | { status: 'submitting' }
  | { status: 'error'; message: string; field: 'capacity' | null }

export function AdminCourseFormPage({ token, courseId, onSaved, onCancel }: AdminCourseFormPageProps) {
  const isEditMode = courseId !== undefined
  const [values, setValues] = useState<CourseFormPayload>(BLANK_VALUES)
  const [originalCapacity, setOriginalCapacity] = useState<number | null>(null)
  const [loadState, setLoadState] = useState<LoadState>(isEditMode ? { status: 'loading' } : { status: 'ready' })
  const [submitState, setSubmitState] = useState<SubmitState>({ status: 'idle' })

  useEffect(() => {
    if (courseId === undefined) {
      return
    }
    let cancelled = false
    setLoadState({ status: 'loading' })
    getCourseDetail(courseId)
      .then((course: Course) => {
        if (cancelled) return
        setValues(toFormValues(course))
        setOriginalCapacity(course.capacity)
        setLoadState({ status: 'ready' })
      })
      .catch((error: unknown) => {
        if (cancelled) return
        const message = error instanceof ApiError ? error.normalized.message : '강좌 정보를 불러오지 못했습니다.'
        setLoadState({ status: 'load-error', message })
      })
    return () => {
      cancelled = true
    }
  }, [courseId])

  // REQ-ADM-006 — 정원 증설 시에만 안내한다. 동일·감소는 대상이 아니다.
  const showCapacityIncreaseNotice =
    isEditMode && originalCapacity !== null && isCapacityIncrease(originalCapacity, values.capacity)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitState({ status: 'submitting' })
    try {
      if (courseId !== undefined) {
        await updateCourse(courseId, values, token)
      } else {
        await createCourse(values, token)
      }
      onSaved()
    } catch (error) {
      const classified = classifyCourseFormError(error)
      setSubmitState({ status: 'error', message: classified.message, field: classified.field })
    }
  }

  if (loadState.status === 'loading') {
    return <p className="text-sm text-neutral-500">불러오는 중…</p>
  }
  if (loadState.status === 'load-error') {
    return <Alert role="alert" tone="error">{loadState.message}</Alert>
  }

  return (
    <Card className="max-w-xl">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">
          {isEditMode ? '강좌 수정' : '강좌 생성'}
        </h2>
        <Label>
          제목
          <Input
            required
            value={values.title}
            onChange={(event) => setValues((current) => ({ ...current, title: event.target.value }))}
          />
        </Label>
        <Label>
          설명
          <Textarea
            value={values.description ?? ''}
            onChange={(event) => setValues((current) => ({ ...current, description: event.target.value }))}
          />
        </Label>
        <Label>
          정원
          <Input
            type="number"
            min={1}
            required
            value={values.capacity}
            onChange={(event) => setValues((current) => ({ ...current, capacity: Number(event.target.value) }))}
            aria-invalid={submitState.status === 'error' && submitState.field === 'capacity'}
          />
        </Label>
        <Label>
          시작 일시
          <Input
            type="datetime-local"
            required
            value={values.startsAt}
            onChange={(event) => setValues((current) => ({ ...current, startsAt: event.target.value }))}
          />
        </Label>
        <Label>
          종료 일시
          <Input
            type="datetime-local"
            required
            value={values.endsAt}
            onChange={(event) => setValues((current) => ({ ...current, endsAt: event.target.value }))}
          />
        </Label>
        {showCapacityIncreaseNotice && (
          <Alert role="status" tone="info">
            정원 증설은 대기자 승격을 유발할 수 있습니다. 승격 결과는 이 화면에 즉시 반영되지 않을 수 있으며, 잠시 후
            다시 조회하면 확정 인원 증가를 확인할 수 있습니다.
          </Alert>
        )}
        {submitState.status === 'error' && <Alert role="alert" tone="error">{submitState.message}</Alert>}
        <div className="flex gap-2">
          <Button type="submit" disabled={submitState.status === 'submitting'}>
            저장
          </Button>
          <Button type="button" variant="outline" onClick={onCancel}>
            취소
          </Button>
        </div>
      </form>
    </Card>
  )
}
