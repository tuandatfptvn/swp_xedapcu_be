package swp.be.vn.bs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp.be.vn.bs.entity.Category;
import swp.be.vn.bs.repository.CategoryRepository;

import java.util.List;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() { return categoryRepository.findAll(); }
    public Category createCategory(Category category) { return categoryRepository.save(category); }
    public void deleteCategory(Integer id) { categoryRepository.deleteById(id); }
}
