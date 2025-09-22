package br.com.technews.service;

import br.com.technews.entity.Tag;
import br.com.technews.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {
    
    private final TagRepository tagRepository;
    
    /**
     * Busca todas as tags ativas
     */
    public List<Tag> findAllActiveTags() {
        return tagRepository.findByIsActiveTrueOrderByNameAsc();
    }
    
    /**
     * Busca tag por ID
     */
    public Optional<Tag> findById(Long id) {
        return tagRepository.findById(id);
    }
    
    /**
     * Busca tag por nome
     */
    public Optional<Tag> findByName(String name) {
        return tagRepository.findByNameIgnoreCase(name);
    }
    
    /**
     * Cria nova tag
     */
    @Transactional
    public Tag createTag(String name, String color, String description) {
        // Verifica se já existe tag com esse nome
        if (tagRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tag com nome '" + name + "' já existe");
        }
        
        Tag tag = Tag.builder()
                .name(name.toLowerCase().trim())
                .color(color)
                .description(description)
                .isActive(true)
                .build();
        
        return tagRepository.save(tag);
    }
    
    /**
     * Atualiza tag existente
     */
    @Transactional
    public Tag updateTag(Long id, String name, String color, String description) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag não encontrada"));
        
        // Verifica se o novo nome já existe em outra tag
        if (!tag.getName().equalsIgnoreCase(name) && tagRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tag com nome '" + name + "' já existe");
        }
        
        tag.setName(name.toLowerCase().trim());
        tag.setColor(color);
        tag.setDescription(description);
        
        return tagRepository.save(tag);
    }
    
    /**
     * Ativa/desativa tag
     */
    @Transactional
    public Tag toggleTagStatus(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag não encontrada"));
        
        tag.setIsActive(!tag.getIsActive());
        return tagRepository.save(tag);
    }
    
    /**
     * Busca tags por texto
     */
    public List<Tag> searchTags(String searchText) {
        return tagRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(searchText);
    }
    
    /**
     * Busca tags mais populares
     */
    public List<Tag> getMostPopularTags(int limit) {
        return tagRepository.findMostPopularTags(limit);
    }
    
    /**
     * Busca tags mais populares (sem limite)
     */
    public List<Tag> getMostPopularTags() {
        return tagRepository.findMostPopularTags();
    }
    
    /**
     * Busca ou cria tags a partir de uma lista de nomes
     */
    @Transactional
    public Set<Tag> findOrCreateTags(Set<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        
        for (String tagName : tagNames) {
            String normalizedName = tagName.toLowerCase().trim();
            
            Optional<Tag> existingTag = tagRepository.findByNameIgnoreCase(normalizedName);
            if (existingTag.isPresent()) {
                tags.add(existingTag.get());
            } else {
                // Cria nova tag com cor padrão
                Tag newTag = Tag.builder()
                        .name(normalizedName)
                        .color("#007bff") // Cor azul padrão
                        .isActive(true)
                        .build();
                tags.add(tagRepository.save(newTag));
            }
        }
        
        return tags;
    }
    
    /**
     * Conta artigos por tag
     */
    public Long countArticlesByTag(Long tagId) {
        return tagRepository.countArticlesByTagId(tagId);
    }
    
    /**
     * Busca tags usadas em artigos publicados
     */
    public List<Tag> getTagsUsedInPublishedArticles() {
        return tagRepository.findTagsUsedInPublishedArticles();
    }
    
    /**
     * Deleta tag (soft delete - apenas desativa)
     */
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag não encontrada"));
        
        tag.setIsActive(false);
        tagRepository.save(tag);
        
        log.info("Tag '{}' foi desativada", tag.getName());
    }
}