package com.bop.youthpick.sync.entity;

/** FULL = 전량 수집(기본, missing 감지 가능) / DELTA = 변경분만 */
public enum BatchMode {
  FULL,
  DELTA
}
