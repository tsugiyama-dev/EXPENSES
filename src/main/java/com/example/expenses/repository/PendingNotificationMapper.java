package com.example.expenses.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.expenses.domain.PendingNotification;

@Mapper
public interface PendingNotificationMapper {

	void insert(PendingNotification notification);

	List<PendingNotification> findUnreadByUserId(@Param("userId") Long userId);

	void markAllReadByUserId(@Param("userId") Long userId);
}
