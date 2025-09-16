package br.com.technews.service;

import br.com.technews.entity.Category;
import br.com.technews.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciamento de categorias
 */
@Service
@Transactional
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    /**
     * Busca todas as categorias com paginação
     */
    @Transactional(readOnly = true)
    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }
    
    /**
     * Busca todas as categorias ativas
     */
    @Transactional(readOnly = true)
    public List<Category> findAllActive() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }
    
    /**
     * Busca categoria por ID
     */
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }
    
    /**
     * Busca categoria por slug
     */
    @Transactional(readOnly = true)
    public Optional<Category> findBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }
    
    /**
     * Salva uma nova categoria
     */
    public Category save(Category category) {
        validateCategory(category);
        
        if (category.getId() == null) {
            category.setCreatedAt(LocalDateTime.now());
        } else {
            category.setUpdatedAt(LocalDateTime.now());
        }
        
        return categoryRepository.save(category);
    }
    
    /**
     * Atualiza uma categoria existente
     */
    public Category update(Long id, Category categoryData) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada com ID: " + id));
        
        // Atualiza os campos
        existingCategory.setName(categoryData.getName());
        existingCategory.setDescription(categoryData.getDescription());
        existingCategory.setColor(categoryData.getColor());
        existingCategory.setActive(categoryData.getActive());
        existingCategory.setUpdatedAt(LocalDateTime.now());
        
        validateCategory(existingCategory);
        
        return categoryRepository.save(existingCategory);
    }
    
    /**
     * Ativa/desativa uma categoria
     */
    public Category toggleActive(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada com ID: " + id));
        
        category.setActive(!category.getActive());
        category.setUpdatedAt(LocalDateTime.now());
        
        return categoryRepository.save(category);
    }
    
    /**
     * Remove uma categoria (soft delete - apenas desativa)
     */
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada com ID: " + id));
        
        // Verifica se a categoria tem artigos associados
        if (category.getArticles() != null && !category.getArticles().isEmpty()) {
            throw new RuntimeException("Não é possível excluir categoria que possui artigos associados. " +
                                     "Desative a categoria ou remova os artigos primeiro.");
        }
        
        categoryRepository.delete(category);
    }
    
    /**
     * Busca categorias por termo de pesquisa
     */
    @Transactional(readOnly = true)
    public List<Category> search(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllActive();
        }
        return categoryRepository.findBySearchTerm(searchTerm.trim());
    }
    
    /**
     * Busca categorias com artigos publicados
     */
    @Transactional(readOnly = true)
    public List<Category> findCategoriesWithPublishedArticles() {
        return categoryRepository.findCategoriesWithPublishedArticles();
    }
    
    /**
     * Valida os dados da categoria
     */
    private void validateCategory(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new RuntimeException("Nome da categoria é obrigatório");
        }
        
        // Verifica se já existe categoria com o mesmo nome
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(category.getName(), category.getId())) {
            throw new RuntimeException("Já existe uma categoria com este nome");
        }
        
        // Verifica se já existe categoria com o mesmo slug
        String slug = generateSlug(category.getName());
        if (categoryRepository.existsBySlugAndIdNot(slug, category.getId())) {
            throw new RuntimeException("Já existe uma categoria com este slug");
        }
        
        // Valida cor se fornecida
        if (category.getColor() != null && !category.getColor().isEmpty()) {
            if (!category.getColor().matches("^#[0-9A-Fa-f]{6}$")) {
                throw new RuntimeException("Cor deve estar no formato hexadecimal (#FFFFFF)");
            }
        }
    }
    
    /**
     * Gera slug a partir do nome
     */
    private String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                  .replaceAll("[^a-z0-9\\s-]", "")
                  .replaceAll("\\s+", "-")
                  .replaceAll("-+", "-")
                  .trim();
    }
    
    /**
     * Conta total de categorias
     */
    @Transactional(readOnly = true)
    public long count() {
        return categoryRepository.count();
    }
    
    /**
     * Conta categorias ativas
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return categoryRepository.findByActiveTrue().size();
    }
}