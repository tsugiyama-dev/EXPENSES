# Spring Boot Expenses Application - Learning & Implementation Guide

**Part 2: Features, Refactoring, and Learning Resources**

---

## Phase 5: Proposed Features with Learning Objectives

Each feature is designed to teach specific design patterns and principles.

### Feature 1: Expense Categories 🎯 BEGINNER

**Business Value:** Organize expenses by type (Travel, Meals, Office Supplies, etc.)

**What You'll Learn:**
- How to design a new domain entity
- One-to-Many relationships
- Repository pattern
- DTO mapping
- Input validation

**Pattern Focus:** Repository Pattern, DTO Pattern

---

#### 5.1.1 Design Overview

```
┌─────────────────────────────────────────────────────────┐
│                    DOMAIN MODEL                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────┐         ┌─────────────────┐  │
│  │ Expense             │         │ Category        │  │
│  ├─────────────────────┤         ├─────────────────┤  │
│  │ - id                │         │ - id            │  │
│  │ - categoryId ───────┼────────>│ - name          │  │
│  │ - title             │         │ - description   │  │
│  │ - amount            │         │ - color         │  │
│  │ ...                 │         │ - icon          │  │
│  └─────────────────────┘         │ - active        │  │
│                                  │ - createdAt     │  │
│                                  └─────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

#### 5.1.2 Step-by-Step Implementation Guide

**STEP 1: Create Domain Entity**

```java
package com.example.expenses.domain;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Category {
    private Long id;
    private String name;           // e.g., "Travel", "Meals"
    private String description;    // Optional description
    private String color;          // Hex color for UI: "#FF5733"
    private String icon;           // Icon name: "airplane", "food"
    private boolean active;        // Soft delete flag
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Business logic (Rich Domain Model!)
    public void deactivate() {
        if (!this.active) {
            throw new IllegalStateException(
                "Category is already inactive"
            );
        }
        this.active = false;
    }

    public void activate() {
        if (this.active) {
            throw new IllegalStateException(
                "Category is already active"
            );
        }
        this.active = true;
    }

    public void updateDetails(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                "Category name cannot be blank"
            );
        }
        this.name = name;
        this.description = description;
    }
}
```

**💡 Learning Point - Rich Domain Model:**
Notice how Category has behavior, not just data:
- `deactivate()` - Business rule: can't deactivate twice
- `activate()` - Business rule: can't activate twice
- `updateDetails()` - Business rule: name is required

This is **better** than having setters and putting validation in service!

**STEP 2: Create DTOs**

```java
package com.example.expenses.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoryCreateRequest(
    @NotBlank(message = "Category name is required")
    String name,

    String description,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$",
             message = "Color must be a valid hex code")
    String color,

    String icon
) {}

public record CategoryUpdateRequest(
    @NotBlank(message = "Category name is required")
    String name,

    String description,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$",
             message = "Color must be a valid hex code")
    String color,

    String icon
) {}
```

```java
package com.example.expenses.dto.response;

import java.time.LocalDateTime;

public record CategoryResponse(
    Long id,
    String name,
    String description,
    String color,
    String icon,
    boolean active,
    LocalDateTime createdAt
) {
    public static CategoryResponse fromDomain(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getColor(),
            category.getIcon(),
            category.isActive(),
            category.getCreatedAt()
        );
    }

    public static List<CategoryResponse> fromDomainList(
        List<Category> categories) {
        return categories.stream()
            .map(CategoryResponse::fromDomain)
            .toList();
    }
}
```

**💡 Learning Point - DTO Pattern:**
- **Request DTOs:** Validate user input at API boundary
- **Response DTOs:** Control what data is exposed
- **Separation:** Can change database without changing API

**STEP 3: Create Repository Interface**

```java
package com.example.expenses.repository;

import com.example.expenses.domain.Category;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CategoryMapper {

    @Insert("""
        INSERT INTO categories (name, description, color, icon, active)
        VALUES (#{name}, #{description}, #{color}, #{icon}, true)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Category category);

    @Select("""
        SELECT id, name, description, color, icon, active,
               created_at, updated_at
        FROM categories
        WHERE id = #{id}
        """)
    Category findById(Long id);

    @Select("""
        SELECT id, name, description, color, icon, active,
               created_at, updated_at
        FROM categories
        WHERE active = true
        ORDER BY name
        """)
    List<Category> findAllActive();

    @Select("""
        SELECT id, name, description, color, icon, active,
               created_at, updated_at
        FROM categories
        ORDER BY name
        """)
    List<Category> findAll();

    @Update("""
        UPDATE categories
        SET name = #{name},
            description = #{description},
            color = #{color},
            icon = #{icon},
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int update(Category category);

    @Update("""
        UPDATE categories
        SET active = #{active}, updated_at = NOW()
        WHERE id = #{id}
        """)
    int updateActiveStatus(@Param("id") Long id,
                          @Param("active") boolean active);
}
```

**💡 Learning Point - Repository Pattern:**
- Repository provides collection-like interface
- Hides SQL details from business logic
- Makes testing easier (can create in-memory version)

**STEP 4: Create Service**

```java
package com.example.expenses.service;

import com.example.expenses.domain.Category;
import com.example.expenses.dto.request.CategoryCreateRequest;
import com.example.expenses.dto.request.CategoryUpdateRequest;
import com.example.expenses.dto.response.CategoryResponse;
import com.example.expenses.repository.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        // Create domain object
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setColor(request.color() != null ? request.color() : "#6B7280");
        category.setIcon(request.icon() != null ? request.icon() : "tag");
        category.setActive(true);

        // Persist
        categoryMapper.insert(category);

        // Return DTO
        return CategoryResponse.fromDomain(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        List<Category> categories = categoryMapper.findAll();
        return CategoryResponse.fromDomainList(categories);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllActive() {
        List<Category> categories = categoryMapper.findAllActive();
        return CategoryResponse.fromDomainList(categories);
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new NoSuchElementException("Category not found: " + id);
        }
        return CategoryResponse.fromDomain(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new NoSuchElementException("Category not found: " + id);
        }

        // Use domain logic!
        category.updateDetails(request.name(), request.description());
        category.setColor(request.color());
        category.setIcon(request.icon());

        categoryMapper.update(category);

        return CategoryResponse.fromDomain(category);
    }

    @Transactional
    public void deactivate(Long id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new NoSuchElementException("Category not found: " + id);
        }

        // Use domain logic!
        category.deactivate();

        categoryMapper.updateActiveStatus(id, false);
    }

    @Transactional
    public void activate(Long id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new NoSuchElementException("Category not found: " + id);
        }

        // Use domain logic!
        category.activate();

        categoryMapper.updateActiveStatus(id, true);
    }
}
```

**💡 Learning Point - Service Layer:**
- Orchestrates domain operations
- Manages transactions
- Converts between DTOs and domain objects
- Notice: Validation is in domain, not here!

**STEP 5: Create Controller**

```java
package com.example.expenses.controller;

import com.example.expenses.dto.request.CategoryCreateRequest;
import com.example.expenses.dto.request.CategoryUpdateRequest;
import com.example.expenses.dto.response.CategoryResponse;
import com.example.expenses.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryCreateRequest request) {

        CategoryResponse response = categoryService.create(request);

        URI location = URI.create("/api/v1/categories/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll(
            @RequestParam(defaultValue = "true") boolean activeOnly) {

        List<CategoryResponse> categories = activeOnly
            ? categoryService.findAllActive()
            : categoryService.findAll();

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
        CategoryResponse category = categoryService.findById(id);
        return ResponseEntity.ok(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {

        CategoryResponse response = categoryService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        categoryService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        categoryService.activate(id);
        return ResponseEntity.noContent().build();
    }
}
```

**💡 Learning Point - REST API Design:**
- `POST /categories` - Create (201 Created + Location header)
- `GET /categories` - List (200 OK)
- `GET /categories/{id}` - Get one (200 OK or 404)
- `PUT /categories/{id}` - Update (200 OK)
- `DELETE /categories/{id}` - Soft delete (204 No Content)

**STEP 6: Create Database Migration**

```sql
-- src/main/resources/db/migration/V9__create_categories.sql
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(7) DEFAULT '#6B7280',
    icon VARCHAR(50) DEFAULT 'tag',
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add category_id to expenses table
ALTER TABLE expenses
ADD COLUMN category_id BIGINT,
ADD CONSTRAINT fk_expense_category
    FOREIGN KEY (category_id) REFERENCES categories(id);

-- Create index for faster lookups
CREATE INDEX idx_expense_category ON expenses(category_id);

-- Insert default categories
INSERT INTO categories (name, description, color, icon) VALUES
('Travel', 'Transportation and travel expenses', '#3B82F6', 'airplane'),
('Meals', 'Food and dining expenses', '#EF4444', 'utensils'),
('Office Supplies', 'Stationery and office materials', '#10B981', 'paperclip'),
('Software', 'Software licenses and subscriptions', '#8B5CF6', 'code'),
('Training', 'Professional development and training', '#F59E0B', 'book'),
('Entertainment', 'Client entertainment and events', '#EC4899', 'ticket'),
('Other', 'Miscellaneous expenses', '#6B7280', 'tag');
```

**💡 Learning Point - Database Design:**
- Foreign key ensures referential integrity
- Index improves query performance
- Default values for better UX
- Unique constraint prevents duplicates

**STEP 7: Write Tests**

```java
package com.example.expenses.service;

import com.example.expenses.domain.Category;
import com.example.expenses.dto.request.CategoryCreateRequest;
import com.example.expenses.dto.response.CategoryResponse;
import com.example.expenses.repository.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_ShouldCreateCategoryWithDefaults() {
        // Given
        CategoryCreateRequest request = new CategoryCreateRequest(
            "Travel",
            "Business travel expenses",
            null,  // No color provided
            null   // No icon provided
        );

        // Mock repository behavior
        doAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(1L);  // Simulate database ID generation
            return null;
        }).when(categoryMapper).insert(any(Category.class));

        // When
        CategoryResponse response = categoryService.create(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Travel");
        assertThat(response.color()).isEqualTo("#6B7280");  // Default
        assertThat(response.icon()).isEqualTo("tag");       // Default
        assertThat(response.active()).isTrue();

        // Verify repository was called
        verify(categoryMapper).insert(any(Category.class));
    }

    @Test
    void deactivate_ShouldThrowException_WhenCategoryNotFound() {
        // Given
        Long categoryId = 999L;
        when(categoryMapper.findById(categoryId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> categoryService.deactivate(categoryId))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Category not found");

        // Verify status was never updated
        verify(categoryMapper, never()).updateActiveStatus(any(), anyBoolean());
    }
}
```

**💡 Learning Point - Testing:**
- Use `@Mock` for dependencies
- Use `@InjectMocks` to inject mocks
- Test both success and failure cases
- Verify method calls with `verify()`

#### 5.1.3 Practice Exercises

Now it's your turn! Try these exercises to reinforce learning:

**Exercise 1: Add Validation**
Add business rule: Category name must be unique.
- Hint: Query database in service before creating
- Throw `BusinessException` if duplicate found

**Exercise 2: Add Expense Count**
Add field to CategoryResponse showing how many expenses use this category.
- Hint: Add method to CategoryMapper: `countExpensesByCategory(Long categoryId)`
- Hint: Use LEFT JOIN in SQL

**Exercise 3: Prevent Deletion of Used Categories**
Don't allow deactivating categories that have associated expenses.
- Where should this check go? (Domain, Service, or Controller?)
- What exception should you throw?

**Exercise 4: Add Category Budget**
Add monthly budget limit to categories.
- What new fields do you need?
- How would you calculate current month spending?

---

### Feature 2: File Upload (Receipt Images) 🎯 INTERMEDIATE

**Business Value:** Attach receipt images/PDFs to expense records for audit purposes.

**What You'll Learn:**
- File handling in Spring Boot
- Strategy pattern for storage (local, S3, Azure)
- Content type validation
- File size limits
- Security considerations

**Pattern Focus:** Strategy Pattern, Adapter Pattern

---

#### 5.2.1 Architecture Design

```
┌────────────────────────────────────────────────────────┐
│              STORAGE STRATEGIES                        │
├────────────────────────────────────────────────────────┤
│                                                        │
│        <<interface>>                                   │
│        FileStorage                                     │
│        + store(file, metadata): FileReference          │
│        + retrieve(fileId): byte[]                      │
│        + delete(fileId): void                          │
│        + getUrl(fileId): String                        │
│               ▲                                        │
│               │                                        │
│       ┌───────┼───────┬──────────┐                    │
│       │       │       │          │                    │
│  ┌────┴───┐ ┌┴────┐ ┌┴─────┐  ┌─┴─────────┐         │
│  │ Local  │ │ S3  │ │Azure │  │ Database  │         │
│  │Storage │ │     │ │Blob  │  │ (Testing) │         │
│  └────────┘ └─────┘ └──────┘  └───────────┘         │
│                                                        │
└────────────────────────────────────────────────────────┘
```

#### 5.2.2 Step-by-Step Implementation

**STEP 1: Define Storage Interface (Strategy Pattern)**

```java
package com.example.expenses.storage;

import java.io.InputStream;

/**
 * Strategy interface for file storage implementations.
 * Allows switching between local, S3, Azure, etc. without changing business logic.
 */
public interface FileStorage {

    /**
     * Store a file and return its reference.
     *
     * @param inputStream File content
     * @param metadata File metadata
     * @return Reference to stored file
     * @throws StorageException if storage fails
     */
    FileReference store(InputStream inputStream, FileMetadata metadata);

    /**
     * Retrieve file content by ID.
     *
     * @param fileId Unique file identifier
     * @return File content as byte array
     * @throws StorageException if retrieval fails
     */
    byte[] retrieve(String fileId);

    /**
     * Delete a file.
     *
     * @param fileId Unique file identifier
     * @throws StorageException if deletion fails
     */
    void delete(String fileId);

    /**
     * Get public URL for file (if supported).
     *
     * @param fileId Unique file identifier
     * @return Public URL or null if not supported
     */
    String getPublicUrl(String fileId);

    /**
     * Check if file exists.
     */
    boolean exists(String fileId);
}
```

**Value Objects:**

```java
package com.example.expenses.storage;

import java.time.LocalDateTime;

public record FileMetadata(
    String originalFilename,
    String contentType,
    long size,
    String uploadedBy
) {
    public FileMetadata {
        // Validation in compact constructor
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be blank");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
        if (size > 10 * 1024 * 1024) {  // 10 MB
            throw new IllegalArgumentException(
                "File size exceeds maximum of 10MB"
            );
        }
    }

    public String getFileExtension() {
        int lastDot = originalFilename.lastIndexOf('.');
        return lastDot > 0 ? originalFilename.substring(lastDot) : "";
    }
}

public record FileReference(
    String fileId,
    String originalFilename,
    String contentType,
    long size,
    String storageLocation,
    LocalDateTime uploadedAt
) {}
```

**💡 Learning Point - Value Objects:**
Records are perfect for value objects:
- Immutable by default
- Automatic equals/hashCode/toString
- Compact constructor for validation
- Self-validating

**STEP 2: Local Storage Implementation**

```java
package com.example.expenses.storage.impl;

import com.example.expenses.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class LocalFileStorage implements FileStorage {

    private final Path storageDirectory;

    public LocalFileStorage(
            @Value("${app.storage.local.directory:./uploads}") String directory) {
        this.storageDirectory = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDirectory);
            log.info("Storage directory created: {}", storageDirectory);
        } catch (IOException e) {
            throw new StorageException("Failed to create storage directory", e);
        }
    }

    @Override
    public FileReference store(InputStream inputStream, FileMetadata metadata) {
        try {
            // Generate unique file ID
            String fileId = UUID.randomUUID().toString();
            String filename = fileId + metadata.getFileExtension();

            // Resolve target path (security check!)
            Path targetPath = storageDirectory.resolve(filename);
            if (!targetPath.startsWith(storageDirectory)) {
                throw new StorageException(
                    "Invalid file path - potential directory traversal attack"
                );
            }

            // Copy file
            long bytesCopied = Files.copy(inputStream, targetPath,
                                         StandardCopyOption.REPLACE_EXISTING);

            if (bytesCopied != metadata.size()) {
                Files.deleteIfExists(targetPath);
                throw new StorageException(
                    "File size mismatch. Expected: " + metadata.size() +
                    ", Actual: " + bytesCopied
                );
            }

            log.info("File stored: id={}, size={}, location={}",
                    fileId, bytesCopied, targetPath);

            return new FileReference(
                fileId,
                metadata.originalFilename(),
                metadata.contentType(),
                metadata.size(),
                targetPath.toString(),
                LocalDateTime.now()
            );

        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }
    }

    @Override
    public byte[] retrieve(String fileId) {
        try {
            Path filePath = findFilePath(fileId);
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new StorageException("Failed to retrieve file: " + fileId, e);
        }
    }

    @Override
    public void delete(String fileId) {
        try {
            Path filePath = findFilePath(fileId);
            Files.delete(filePath);
            log.info("File deleted: {}", fileId);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file: " + fileId, e);
        }
    }

    @Override
    public String getPublicUrl(String fileId) {
        // Local storage doesn't support public URLs
        return null;
    }

    @Override
    public boolean exists(String fileId) {
        try {
            Path filePath = findFilePath(fileId);
            return Files.exists(filePath);
        } catch (StorageException e) {
            return false;
        }
    }

    private Path findFilePath(String fileId) {
        try {
            // Search for file with this ID (any extension)
            return Files.list(storageDirectory)
                .filter(path -> path.getFileName().toString().startsWith(fileId))
                .findFirst()
                .orElseThrow(() -> new StorageException(
                    "File not found: " + fileId
                ));
        } catch (IOException e) {
            throw new StorageException("Failed to list directory", e);
        }
    }
}
```

**💡 Learning Point - Security:**
- Always validate file paths to prevent directory traversal
- Verify file size matches uploaded bytes
- Use UUID for file IDs (not user-provided filenames!)
- Store files outside web root

**STEP 3: S3 Storage Implementation (Strategy)**

```java
package com.example.expenses.storage.impl;

import com.example.expenses.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final String bucketName;

    public S3FileStorage(S3Client s3Client,
                        @Value("${app.storage.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public FileReference store(InputStream inputStream, FileMetadata metadata) {
        try {
            String fileId = UUID.randomUUID().toString();
            String key = fileId + metadata.getFileExtension();

            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(metadata.contentType())
                .contentLength(metadata.size())
                .metadata(Map.of(
                    "original-filename", metadata.originalFilename(),
                    "uploaded-by", metadata.uploadedBy()
                ))
                .build();

            s3Client.putObject(request,
                RequestBody.fromInputStream(inputStream, metadata.size()));

            log.info("File uploaded to S3: bucket={}, key={}", bucketName, key);

            return new FileReference(
                fileId,
                metadata.originalFilename(),
                metadata.contentType(),
                metadata.size(),
                "s3://" + bucketName + "/" + key,
                LocalDateTime.now()
            );

        } catch (S3Exception e) {
            throw new StorageException("Failed to upload to S3", e);
        }
    }

    @Override
    public byte[] retrieve(String fileId) {
        try {
            String key = findKeyByFileId(fileId);

            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            return s3Client.getObjectAsBytes(request).asByteArray();

        } catch (S3Exception e) {
            throw new StorageException("Failed to retrieve from S3", e);
        }
    }

    @Override
    public void delete(String fileId) {
        try {
            String key = findKeyByFileId(fileId);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            s3Client.deleteObject(request);
            log.info("File deleted from S3: bucket={}, key={}", bucketName, key);

        } catch (S3Exception e) {
            throw new StorageException("Failed to delete from S3", e);
        }
    }

    @Override
    public String getPublicUrl(String fileId) {
        String key = findKeyByFileId(fileId);
        // Generate presigned URL valid for 1 hour
        return s3Client.utilities()
            .getUrl(builder -> builder.bucket(bucketName).key(key))
            .toExternalForm();
    }

    @Override
    public boolean exists(String fileId) {
        try {
            String key = findKeyByFileId(fileId);
            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    private String findKeyByFileId(String fileId) {
        // List objects with prefix and find matching key
        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(fileId)
            .maxKeys(1)
            .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        return response.contents().stream()
            .findFirst()
            .map(S3Object::key)
            .orElseThrow(() -> new StorageException("File not found in S3: " + fileId));
    }
}
```

**💡 Learning Point - Strategy Pattern Benefits:**
1. **Switch storage** by changing `app.storage.type` property
2. **Test locally** with LocalFileStorage
3. **Deploy to production** with S3FileStorage
4. **Business logic unchanged** - same interface!

**Configuration:**
```yaml
# application.yml
app:
  storage:
    type: local  # or 's3' or 'azure'
    local:
      directory: ./uploads
    s3:
      bucket: my-expenses-bucket
      region: us-east-1
```

**STEP 4: Create Receipt Entity and Repository**

```java
package com.example.expenses.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Receipt {
    private Long id;
    private Long expenseId;
    private String fileId;              // From FileStorage
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private String storageLocation;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime deletedAt;    // Soft delete
}
```

```java
package com.example.expenses.repository;

import com.example.expenses.domain.Receipt;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ReceiptMapper {

    @Insert("""
        INSERT INTO receipts
            (expense_id, file_id, original_filename, content_type,
             file_size, storage_location, uploaded_by)
        VALUES
            (#{expenseId}, #{fileId}, #{originalFilename}, #{contentType},
             #{fileSize}, #{storageLocation}, #{uploadedBy})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Receipt receipt);

    @Select("""
        SELECT id, expense_id, file_id, original_filename, content_type,
               file_size, storage_location, uploaded_by, uploaded_at, deleted_at
        FROM receipts
        WHERE expense_id = #{expenseId} AND deleted_at IS NULL
        ORDER BY uploaded_at DESC
        """)
    List<Receipt> findByExpenseId(Long expenseId);

    @Select("""
        SELECT id, expense_id, file_id, original_filename, content_type,
               file_size, storage_location, uploaded_by, uploaded_at, deleted_at
        FROM receipts
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    Receipt findById(Long id);

    @Update("""
        UPDATE receipts
        SET deleted_at = NOW()
        WHERE id = #{id}
        """)
    int softDelete(Long id);
}
```

**Database Migration:**
```sql
-- V10__create_receipts.sql
CREATE TABLE receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    file_id VARCHAR(255) NOT NULL UNIQUE,
    original_filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_location VARCHAR(1000) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id),
    INDEX idx_expense_id (expense_id),
    INDEX idx_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**STEP 5: Create Service with Business Logic**

```java
package com.example.expenses.service;

import com.example.expenses.domain.Receipt;
import com.example.expenses.repository.ReceiptMapper;
import com.example.expenses.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final FileStorage fileStorage;
    private final ReceiptMapper receiptMapper;

    // Allowed content types
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/gif",
        "application/pdf"
    );

    // Max file size: 10 MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Transactional
    public Receipt uploadReceipt(Long expenseId, MultipartFile file, Long userId) {

        // Validate file
        validateFile(file);

        try {
            // Create metadata
            FileMetadata metadata = new FileMetadata(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                String.valueOf(userId)
            );

            // Store file using strategy
            FileReference fileRef = fileStorage.store(
                file.getInputStream(),
                metadata
            );

            // Save receipt record
            Receipt receipt = new Receipt();
            receipt.setExpenseId(expenseId);
            receipt.setFileId(fileRef.fileId());
            receipt.setOriginalFilename(fileRef.originalFilename());
            receipt.setContentType(fileRef.contentType());
            receipt.setFileSize(fileRef.size());
            receipt.setStorageLocation(fileRef.storageLocation());
            receipt.setUploadedBy(userId);

            receiptMapper.insert(receipt);

            log.info("Receipt uploaded: expenseId={}, receiptId={}, fileId={}",
                    expenseId, receipt.getId(), fileRef.fileId());

            return receipt;

        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Receipt> getReceiptsByExpense(Long expenseId) {
        return receiptMapper.findByExpenseId(expenseId);
    }

    @Transactional(readOnly = true)
    public byte[] downloadReceipt(Long receiptId, Long userId) {
        Receipt receipt = receiptMapper.findById(receiptId);
        if (receipt == null) {
            throw new NoSuchElementException("Receipt not found: " + receiptId);
        }

        // TODO: Add authorization check (owner or approver)

        return fileStorage.retrieve(receipt.getFileId());
    }

    @Transactional
    public void deleteReceipt(Long receiptId, Long userId) {
        Receipt receipt = receiptMapper.findById(receiptId);
        if (receipt == null) {
            throw new NoSuchElementException("Receipt not found: " + receiptId);
        }

        // TODO: Add authorization check (owner only)

        // Soft delete in database
        receiptMapper.softDelete(receiptId);

        // Delete from storage (could be async)
        try {
            fileStorage.delete(receipt.getFileId());
        } catch (StorageException e) {
            log.error("Failed to delete file from storage: {}", receipt.getFileId(), e);
            // Don't fail the request - file is already marked as deleted in DB
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                "File size exceeds maximum of 10MB"
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                "Invalid file type. Allowed: JPEG, PNG, GIF, PDF"
            );
        }

        // Check file extension matches content type
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            boolean extensionMatchesType =
                (contentType.contains("jpeg") && extension.equals("jpg")) ||
                (contentType.contains("jpeg") && extension.equals("jpeg")) ||
                (contentType.contains("png") && extension.equals("png")) ||
                (contentType.contains("gif") && extension.equals("gif")) ||
                (contentType.contains("pdf") && extension.equals("pdf"));

            if (!extensionMatchesType) {
                throw new IllegalArgumentException(
                    "File extension doesn't match content type"
                );
            }
        }
    }
}
```

**💡 Learning Point - File Upload Security:**
1. ✅ Validate content type (MIME type)
2. ✅ Validate file extension matches content type
3. ✅ Limit file size
4. ✅ Use UUID for storage (not user filenames)
5. ✅ Store outside web root
6. ✅ Validate paths to prevent directory traversal
7. ✅ Scan for viruses (production systems)

**STEP 6: Create Controller**

```java
package com.example.expenses.controller;

import com.example.expenses.domain.Receipt;
import com.example.expenses.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses/{expenseId}/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Receipt> uploadReceipt(
            @PathVariable Long expenseId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute Long userId) {  // From auth filter

        Receipt receipt = receiptService.uploadReceipt(expenseId, file, userId);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping
    public ResponseEntity<List<Receipt>> getReceipts(
            @PathVariable Long expenseId) {

        List<Receipt> receipts = receiptService.getReceiptsByExpense(expenseId);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/{receiptId}/download")
    public ResponseEntity<Resource> downloadReceipt(
            @PathVariable Long expenseId,
            @PathVariable Long receiptId,
            @RequestAttribute Long userId) {

        byte[] fileData = receiptService.downloadReceipt(receiptId, userId);
        Receipt receipt = receiptService.getReceiptsByExpense(expenseId).stream()
            .filter(r -> r.getId().equals(receiptId))
            .findFirst()
            .orElseThrow();

        ByteArrayResource resource = new ByteArrayResource(fileData);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(receipt.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                   "attachment; filename=\"" + receipt.getOriginalFilename() + "\"")
            .body(resource);
    }

    @DeleteMapping("/{receiptId}")
    public ResponseEntity<Void> deleteReceipt(
            @PathVariable Long expenseId,
            @PathVariable Long receiptId,
            @RequestAttribute Long userId) {

        receiptService.deleteReceipt(receiptId, userId);
        return ResponseEntity.noContent().build();
    }
}
```

#### 5.2.3 Practice Exercises

**Exercise 1: Add Image Thumbnails**
Generate thumbnails for uploaded images (e.g., 200x200px).
- Use Java ImageIO or library like Thumbnailator
- Store thumbnail alongside original
- Return thumbnail URL in receipt list

**Exercise 2: Add Virus Scanning**
Integrate ClamAV or similar to scan uploads.
- Create `VirusScannerService` interface
- Implement with ClamAV
- Scan before storing file
- Reject infected files

**Exercise 3: Add Signed URLs**
Generate temporary download URLs that expire.
- Add method `String getSignedUrl(String fileId, Duration validity)`
- Implement for S3 (presigned URLs)
- Implement for local (JWT token in URL?)

**Exercise 4: Add Multiple File Upload**
Allow uploading multiple receipts at once.
- Change endpoint to accept `List<MultipartFile>`
- Return `List<Receipt>`
- Make it transactional (all or nothing)

---

### Feature 3: Export to Multiple Formats 🎯 INTERMEDIATE

**Business Value:** Export expense data to CSV, Excel, and PDF for reporting.

**What You'll Learn:**
- Factory pattern
- Strategy pattern
- Template method pattern
- Builder pattern for complex objects

**Pattern Focus:** Strategy + Factory + Template Method

---

#### 5.3.1 Design

```
┌──────────────────────────────────────────────────────┐
│         EXPORT ARCHITECTURE                          │
├──────────────────────────────────────────────────────┤
│                                                      │
│  Client                                              │
│    │                                                 │
│    ▼                                                 │
│  ExpenseExportService                                │
│    │                                                 │
│    ├──> ExportStrategyFactory                        │
│    │      └──> getStrategy(format): ExportStrategy   │
│    │                                                 │
│    └──> ExportStrategy.export(data)                  │
│                  ▲                                   │
│                  │                                   │
│         ┌────────┼────────┬────────┐                │
│         │        │        │        │                │
│    ┌────┴──┐ ┌──┴───┐ ┌──┴───┐ ┌──┴─────┐          │
│    │  CSV  │ │Excel │ │ PDF  │ │ JSON   │          │
│    │Strategy│ │      │ │      │ │        │          │
│    └───────┘ └──────┘ └──────┘ └────────┘          │
│                                                      │
└──────────────────────────────────────────────────────┘
```

**Implementation sketch** (full code similar to previous examples):

```java
// 1. Strategy interface
public interface ExportStrategy {
    byte[] export(List<Expense> expenses);
    String getContentType();
    String getFileExtension();
}

// 2. Factory
@Component
public class ExportStrategyFactory {
    private final Map<ExportFormat, ExportStrategy> strategies;

    public ExportStrategyFactory(List<ExportStrategy> allStrategies) {
        // Spring injects all ExportStrategy beans
        this.strategies = allStrategies.stream()
            .collect(Collectors.toMap(
                s -> ExportFormat.fromExtension(s.getFileExtension()),
                s -> s
            ));
    }

    public ExportStrategy getStrategy(ExportFormat format) {
        ExportStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new IllegalArgumentException(
                "Unsupported export format: " + format
            );
        }
        return strategy;
    }
}

// 3. Concrete strategies
@Component
public class CsvExportStrategy implements ExportStrategy { ... }

@Component
public class ExcelExportStrategy implements ExportStrategy { ... }

@Component
public class PdfExportStrategy implements ExportStrategy { ... }

// 4. Service using factory
@Service
public class ExpenseExportService {
    private final ExportStrategyFactory factory;
    private final ExpenseSearchService searchService;

    public byte[] export(ExpenseSearchCriteria criteria, ExportFormat format) {
        // Get data
        List<Expense> expenses = searchService.findByCriteria(criteria);

        // Get appropriate strategy
        ExportStrategy strategy = factory.getStrategy(format);

        // Execute export
        return strategy.export(expenses);
    }
}
```

---

### Feature 4: Email Notifications (Enhanced) 🎯 INTERMEDIATE

**Pattern Focus:** Observer Pattern (Event-Driven Architecture)

Refactor existing notifications to use Spring Events (shown in Phase 2).

**Benefits:**
- ✅ Loose coupling
- ✅ Easy to add new listeners (Slack, SMS, etc.)
- ✅ Async processing
- ✅ Testability

---

### Feature 5: Multi-Level Approval Workflow 🎯 ADVANCED

**Business Value:** Require multiple approvals for high-value expenses.

**What You'll Learn:**
- State pattern
- Chain of Responsibility pattern
- Workflow design
- Complex state machines

**Pattern Focus:** State + Chain of Responsibility

---

#### 5.5.1 State Diagram

```
                    ┌──────────┐
                    │  DRAFT   │
                    └────┬─────┘
                         │ submit()
                         ▼
                  ┌────────────┐
                  │ SUBMITTED  │
                  └─────┬──────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
  ┌───────────┐  ┌─────────────┐  ┌────────┐
  │ REJECTED  │  │ LEVEL1_      │  │ AUTO   │
  │           │  │ APPROVED     │  │ APPROVED│
  └───────────┘  └──────┬──────┘  └────────┘
                        │
                        ▼
                 ┌─────────────┐
                 │ LEVEL2_     │
                 │ APPROVED    │
                 └──────┬──────┘
                        │
                        ▼
                 ┌───────────┐
                 │ FULLY_    │
                 │ APPROVED  │
                 └───────────┘
```

**Rules:**
- Expenses < $100: Auto-approved
- Expenses $100-$1000: Requires 1 approval
- Expenses > $1000: Requires 2 approvals

**Implementation uses:**
1. **State Pattern:** Each state is a class
2. **Chain of Responsibility:** Approval handlers chained
3. **Specification Pattern:** Rules for approval requirements

---

### Feature 6: Budget Management 🎯 ADVANCED

**Pattern Focus:** Specification Pattern, Observer Pattern

Track spending against budgets:
- Set monthly/quarterly budgets by category
- Alert when nearing limit (e.g., 80%, 90%, 100%)
- Prevent submissions over budget
- Dashboard showing budget utilization

---

### Feature 7: Recurring Expenses 🎯 ADVANCED

**Pattern Focus:** Template Method Pattern, Strategy Pattern

Support subscriptions and recurring expenses:
- Monthly, quarterly, annual recurrence
- Auto-generate expense records
- Email reminders before recurrence
- Batch processing with scheduled jobs

---

### Feature 8: Analytics & Reports 🎯 ADVANCED

**Pattern Focus:** Builder Pattern, Strategy Pattern, Template Method

Generate various reports:
- Spending by category
- Spending by user
- Spending trends over time
- Top expenses
- Pending approvals

Use Builder pattern for complex report configurations.

---

## Phase 6: Refactoring Roadmap

Priority-ordered plan to improve the codebase.

### 6.1 Quick Wins (1-2 weeks)

#### Refactor 1: Extract AuthenticationContext Service 🟢

**Current Problem:** Static `CurrentUser` utility

**Effort:** 2-3 hours
**Impact:** HIGH (enables testing)
**Learning:** Dependency Injection, Testability

**Steps:**
1. Create `AuthenticationContext` service
2. Inject into services
3. Replace all `CurrentUser.actorId()` calls
4. Write tests with mocked context

**Files to change:**
- Create: `src/main/java/com/example/expenses/security/AuthenticationContext.java`
- Modify: `ExpenseService.java`, all services using `CurrentUser`
- Delete: `CurrentUser.java`

#### Refactor 2: Add Service Interfaces 🟢

**Effort:** 1 day
**Impact:** MEDIUM (better testability)

**Steps:**
1. Create `IExpenseService` interface
2. Rename `ExpenseService` → `ExpenseServiceImpl`
3. Update controller to depend on interface
4. Repeat for other services

#### Refactor 3: Extract Export Logic to Strategy Pattern 🟢

**Effort:** 1 day
**Impact:** HIGH (extensibility)

**Steps:**
1. Create `ExportStrategy` interface
2. Create `CsvExportStrategy`
3. Create `ExportStrategyFactory`
4. Refactor `ExpenseService.getCsv()` to use strategy

---

### 6.2 Medium-Term Improvements (2-4 weeks)

#### Refactor 4: Split ExpenseService into Focused Services 🟡

**Effort:** 3-4 days
**Impact:** VERY HIGH (maintainability)

**New Services:**
- `ExpenseCreationService`
- `ExpenseWorkflowService` (submit, approve, reject)
- `ExpenseSearchService`
- `ExpenseExportService`

#### Refactor 5: Implement Event-Driven Notifications 🟡

**Effort:** 2-3 days
**Impact:** HIGH (loose coupling)

**Steps:**
1. Create domain events (`ExpenseApprovedEvent`, etc.)
2. Publish events in service
3. Create event listeners
4. Refactor `NotificationService` to listen to events

#### Refactor 6: Add Repository Abstraction Layer 🟡

**Effort:** 3-5 days
**Impact:** MEDIUM (flexibility)

**Steps:**
1. Create `ExpenseRepository` interface
2. Create `MyBatisExpenseRepository` implementation
3. Update services to depend on repository interface

#### Refactor 7: Implement Rich Domain Model 🟡

**Effort:** 5-7 days
**Impact:** VERY HIGH (code quality)

**Steps:**
1. Add business methods to `Expense`
2. Add validation to domain objects
3. Move business logic from service to domain
4. Refactor services to orchestrate, not contain logic

---

### 6.3 Long-Term Improvements (1-2 months)

#### Refactor 8: Implement Specification Pattern for Search 🔴

**Effort:** 1 week
**Impact:** MEDIUM (reusability)

#### Refactor 9: Add Caching Layer 🔴

**Effort:** 1 week
**Impact:** MEDIUM (performance)

#### Refactor 10: API Versioning 🔴

**Effort:** 2-3 days
**Impact:** MEDIUM (future-proofing)

---

### 6.4 Refactoring Priority Matrix

```
┌──────────────────────────────────────────────────────┐
│  Impact                                              │
│    ▲                                                 │
│ H  │  ┌────────────┐  ┌────────────┐               │
│ I  │  │ Rich Domain│  │ Event-     │               │
│ G  │  │ Model      │  │ Driven     │               │
│ H  │  └────────────┘  └────────────┘               │
│    │                                                 │
│    │  ┌────────────┐  ┌────────────┐               │
│ M  │  │ Split      │  │ Repository │               │
│ E  │  │ Services   │  │ Abstraction│               │
│ D  │  └────────────┘  └────────────┘               │
│    │                                                 │
│    │  ┌────────────┐  ┌────────────┐               │
│ L  │  │ API        │  │ Caching    │               │
│ O  │  │ Versioning │  │            │               │
│ W  │  └────────────┘  └────────────┘               │
│    │                                                 │
│    └──────────┬──────────┬──────────┬──────────────>
│             LOW        MEDIUM      HIGH     Effort  │
└──────────────────────────────────────────────────────┘

Start with: High Impact + Low/Medium Effort (top left)
```

---

## Phase 7: Learning Resources

### 7.1 Core Concepts Reference

#### Access Modifiers in Java

| Modifier | Same Class | Same Package | Subclass | Everywhere |
|----------|------------|--------------|----------|------------|
| `public` | ✅ | ✅ | ✅ | ✅ |
| `protected` | ✅ | ✅ | ✅ | ❌ |
| package-private (no modifier) | ✅ | ✅ | ❌ | ❌ |
| `private` | ✅ | ❌ | ❌ | ❌ |

**When to use each:**

```java
public class Expense {
    // PUBLIC: API that others depend on
    public Long getId() { return id; }

    // PROTECTED: For subclasses (rare in Spring)
    protected void validate() { ... }

    // PACKAGE-PRIVATE: Within same package only
    void setAuditInfo(AuditInfo info) { ... }

    // PRIVATE: Internal implementation
    private boolean isAmountReasonable() { ... }
}
```

**Best Practice:** Start with most restrictive (private), widen only when needed.

#### Interfaces vs Abstract Classes

| Feature | Interface | Abstract Class |
|---------|-----------|----------------|
| Multiple inheritance | ✅ Yes | ❌ No |
| State (fields) | ❌ No (except static final) | ✅ Yes |
| Constructor | ❌ No | ✅ Yes |
| Default implementations | ✅ Yes (Java 8+) | ✅ Yes |
| When to use | Pure behavior contract | Partial implementation + state |

**Example:**

```java
// INTERFACE: Pure contract, no state
public interface PaymentProcessor {
    PaymentResult process(Payment payment);
    void refund(String transactionId);
}

// ABSTRACT CLASS: Shared state + partial implementation
public abstract class BaseEntity {
    private Long id;  // Shared state
    private LocalDateTime createdAt;

    // Shared behavior
    public boolean isNew() {
        return id == null;
    }

    // Force subclasses to implement
    public abstract void validate();
}
```

**When to choose:**
- **Interface:** Defining "can do" relationships (Drawable, Serializable, Comparable)
- **Abstract Class:** Sharing code among related classes (Animal → Dog/Cat)

### 7.2 Design Pattern Cheat Sheet

| Pattern | Problem | Solution | When to Use |
|---------|---------|----------|-------------|
| **Strategy** | Multiple algorithms for same operation | Encapsulate each algorithm | When you need to switch algorithms at runtime |
| **Factory** | Complex object creation | Centralize creation logic | When creation involves multiple steps or decisions |
| **Observer** | Multiple objects react to events | Publish events, objects subscribe | When you need loose coupling between components |
| **Repository** | Data access scattered | Abstract data operations | Always! For all database access |
| **Builder** | Many constructor parameters | Fluent API for construction | When objects have 4+ optional parameters |
| **Template Method** | Similar algorithms with variations | Define skeleton, override steps | When multiple classes share algorithm structure |
| **Specification** | Complex business rules | Combinable rule objects | When you need to combine/reuse business rules |

### 7.3 Recommended Reading Order

**Week 1-2: Fundamentals**
1. Review SOLID principles (this document Phase 3)
2. Study your current codebase layers (Phase 1)
3. Identify code smells (Phase 4)

**Week 3-4: Patterns in Practice**
1. Implement Category feature (Phase 5.1) - Practice Repository + DTO
2. Study Strategy pattern (Phase 2)
3. Refactor export to Strategy (Phase 6, Refactor 3)

**Week 5-6: Advanced Patterns**
1. Implement File Upload (Phase 5.2) - Practice Strategy + Adapter
2. Study Observer pattern (Phase 2)
3. Implement Event-Driven Notifications (Phase 6, Refactor 5)

**Week 7-8: Domain-Driven Design**
1. Study Rich Domain Model (Phase 4.1)
2. Refactor Expense entity (Phase 6, Refactor 7)
3. Implement Specification pattern for search

**Week 9-10: Architecture**
1. Split services (Phase 6, Refactor 4)
2. Add Repository abstraction (Phase 6, Refactor 6)
3. Review and document architecture

### 7.4 Books & Resources

**Essential Reading:**
1. **"Clean Code"** by Robert Martin - Code quality fundamentals
2. **"Refactoring"** by Martin Fowler - How to improve code safely
3. **"Design Patterns"** by Gang of Four - Original pattern catalog
4. **"Domain-Driven Design"** by Eric Evans - Rich domain models

**Spring-Specific:**
1. **"Spring in Action"** by Craig Walls - Spring Boot best practices
2. **Spring.io Guides** - Official tutorials and guides

**Online:**
1. **Refactoring Guru** - refactoring.guru - Visual pattern explanations
2. **Baeldung** - baeldung.com - Spring tutorials
3. **This codebase!** - Your best learning resource

### 7.5 Practice Exercises Checklist

After completing this guide, you should be able to:

- [ ] Explain the difference between a service and a repository
- [ ] Identify which SOLID principle is violated by looking at code
- [ ] Implement Strategy pattern from scratch
- [ ] Create a rich domain model with business logic
- [ ] Write unit tests with mocked dependencies
- [ ] Design a REST API following best practices
- [ ] Use DTOs to decouple API from domain
- [ ] Implement Event-Driven architecture with Spring Events
- [ ] Refactor a god class into focused services
- [ ] Add a new feature without modifying existing code (OCP)

### 7.6 Code Review Checklist

When reviewing your own code, ask:

**SOLID Compliance:**
- [ ] Does each class have a single responsibility?
- [ ] Can I extend without modifying? (interfaces, strategies)
- [ ] Are dependencies injected (not static)?
- [ ] Do interfaces have focused contracts?

**Design Patterns:**
- [ ] Is Repository pattern used for all data access?
- [ ] Are DTOs used at API boundaries?
- [ ] Could Strategy pattern eliminate if/else chains?
- [ ] Should events be used instead of direct calls?

**Code Quality:**
- [ ] Are classes and methods small and focused?
- [ ] Is business logic in domain objects, not services?
- [ ] Are there primitive obsessions (use value objects)?
- [ ] Is there duplication that could be extracted?

**Testing:**
- [ ] Can I test this without a database?
- [ ] Are dependencies mockable?
- [ ] Does each test verify one behavior?
- [ ] Are test names descriptive?

**Security:**
- [ ] Is user input validated?
- [ ] Are SQL queries parameterized?
- [ ] Is authorization checked?
- [ ] Are files validated before storage?

---

## Conclusion

You've built a solid foundation! This guide provides:

1. **Understanding:** Architecture analysis and pattern identification
2. **Learning:** Detailed explanations of principles and patterns
3. **Practice:** 8 feature implementations with step-by-step guides
4. **Improvement:** Prioritized refactoring roadmap
5. **Resources:** Books, articles, and exercises

**Next Steps:**
1. Read through both documents completely
2. Choose one Quick Win refactoring (Phase 6.1)
3. Implement one new feature (start with Categories - Phase 5.1)
4. Review your changes against the code quality checklist
5. Repeat!

**Remember:** Good design is learned through practice, not just reading. The best way to learn is to:
- Try implementing a pattern
- Make mistakes
- Refactor
- Repeat

Your codebase is your learning lab. Experiment, break things, and learn!

Good luck on your learning journey! 🚀
