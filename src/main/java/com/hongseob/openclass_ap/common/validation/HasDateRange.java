package com.hongseob.openclass_ap.common.validation;

import java.time.LocalDateTime;

/**
 * 시작·종료 일시를 갖는 요청 DTO가 구현하는 인터페이스.
 *
 * <p>{@link ValidDateRange}가 필드명(리플렉션)에 의존하지 않고 종료 일시가
 * 시작 일시보다 이른지 검증할 수 있도록 하는 계약이다. Java record는 컴포넌트와
 * 동일한 이름의 접근자 메서드를 자동 생성하므로, {@code startsAt}·{@code endsAt}
 * 컴포넌트를 가진 record가 이 인터페이스를 {@code implements}하는 데 별도 구현
 * 코드가 필요 없다(REQ-NFR-001).</p>
 */
public interface HasDateRange {

    LocalDateTime startsAt();

    LocalDateTime endsAt();
}
