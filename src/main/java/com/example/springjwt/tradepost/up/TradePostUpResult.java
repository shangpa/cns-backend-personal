package com.example.springjwt.tradepost.up;
import java.time.LocalDateTime;

public record TradePostUpResult(Long postId, int usedPoints, String reason, LocalDateTime uppedAt) {}
