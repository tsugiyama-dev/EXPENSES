package com.example.expenses.batch.partitioner;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

public class RangePartitioner implements Partitioner{

	
	private static final String PARTITION_KEY = "partition";

	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		
		// gridSize 分割数（スレッド数）
		 Map<String, ExecutionContext> result = new HashMap<>();
		 
		 //DBから最小IDと最大IDを取得する
		 // ここでは簡易的に固定値を使用
		 long minId = 1;
		 long maxId = 1000;
		 
		 long rangeSize = (maxId - minId + 1) / gridSize; // 各パーティションの範囲
		 for ( int i = 0; i < gridSize; i++) {
			 ExecutionContext context = new ExecutionContext();
			 
			 long startId = minId + (rangeSize * i);
			 long endId = (i == gridSize - 1) ? maxId : startId + rangeSize -1;
			 
			 context.putLong("minId", startId);
			 context.putLong("maxId", endId);
			 context.putString(PARTITION_KEY, PARTITION_KEY + i);
			 
			 result.put(PARTITION_KEY + i, context);
			 
			 System.out.println("Partition " + i + ": ID " + startId + " ~ " + endId);
		 }
		 return result;
	}
	
}
