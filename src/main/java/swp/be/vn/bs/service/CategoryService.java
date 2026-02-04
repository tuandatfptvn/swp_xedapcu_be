package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp.be.vn.bs.dto.request.CategoryRequest;
import swp.be.vn.bs.dto.response.CategoryRespond;
import swp.be.vn.bs.entity.Category;
import swp.be.vn.bs.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    public List<CategoryRespond> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToRespond)
                .collect(Collectors.toList());
    }

    public CategoryRespond createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getCategoryName());

        Category savedCategory = categoryRepository.save(category);
        return mapToRespond(savedCategory);
    }

    public void deleteCategory(Integer id) {
        categoryRepository.deleteById(id);
    }

    private CategoryRespond mapToRespond(Category category) {
        return CategoryRespond.builder()

                .id(category.getCategoryId())
                .categoryName(category.getName())
                .build();
    }
}