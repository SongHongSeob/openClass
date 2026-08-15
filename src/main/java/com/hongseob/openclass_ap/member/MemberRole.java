package com.hongseob.openclass_ap.member;

/**
 * 회원 역할. v1 범위에서는 MEMBER / ADMIN 2종만 존재한다 (spec.md §D — 권한 모델 고도화 제외).
 */
public enum MemberRole {
    MEMBER,
    ADMIN
}
