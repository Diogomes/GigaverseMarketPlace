package br.com.gigaverse3d.catalog.service;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminProductResponse;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.domain.ProductImage;
import br.com.gigaverse3d.catalog.repository.ProductRepository;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.domain.AfterTransaction;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.inventory.repository.InventoryItemRepository;
import br.com.gigaverse3d.storage.StorageService;

@Service
@Transactional
public class ProductImageService {

	public static final int MAX_IMAGES_PER_PRODUCT = 8;

	private final ProductRepository products;

	private final InventoryItemRepository inventory;

	private final ImageValidator validator;

	private final StorageService storage;

	private final CatalogMapper mapper;

	private final AuditService audit;

	public ProductImageService(ProductRepository products, InventoryItemRepository inventory, ImageValidator validator,
			StorageService storage, CatalogMapper mapper, AuditService audit) {
		this.products = products;
		this.inventory = inventory;
		this.validator = validator;
		this.storage = storage;
		this.mapper = mapper;
		this.audit = audit;
	}

	public AdminProductResponse upload(Long productId, String originalFilename, byte[] bytes, String altText) {
		Product product = products.findById(productId)
			.orElseThrow(() -> new NotFoundException("Produto não encontrado."));
		if (altText == null || altText.isBlank() || altText.length() > 160) {
			throw new FieldValidationException("altText", "Descreva a imagem (até 160 caracteres) para acessibilidade.");
		}
		if (product.getImages().size() >= MAX_IMAGES_PER_PRODUCT) {
			throw new ConflictException("Limite de " + MAX_IMAGES_PER_PRODUCT + " imagens por produto atingido.");
		}
		ImageValidator.ValidatedImage image = validator.validate(originalFilename, bytes);
		// Nome gerado pelo servidor: nunca usa o nome enviado pelo cliente.
		String key = "products/" + productId + "/" + UUID.randomUUID() + "." + image.type().extension;
		storage.store(key, new ByteArrayInputStream(bytes), bytes.length, image.type().contentType);
		AfterTransaction.onRollback(() -> storage.delete(key));
		ProductImage saved = product.addImage(key, altText.strip(), image.type().contentType, bytes.length,
				image.width(), image.height());
		// flush (e não save/merge): mantém a mesma instância da imagem e atribui o id
		products.flush();
		audit.record("PRODUCT_IMAGE_ADDED", "product", productId,
				Map.of("imageId", saved.getId(), "contentType", image.type().contentType, "sizeBytes", bytes.length));
		return mapper.admin(product, inventory.findById(productId).orElse(null));
	}

	public void delete(Long productId, Long imageId) {
		Product product = products.findById(productId)
			.orElseThrow(() -> new NotFoundException("Produto não encontrado."));
		ProductImage image = product.getImages()
			.stream()
			.filter(i -> i.getId().equals(imageId))
			.findFirst()
			.orElseThrow(() -> new NotFoundException("Imagem não encontrada."));
		String key = image.getStorageKey();
		product.removeImage(image);
		audit.record("PRODUCT_IMAGE_REMOVED", "product", productId, Map.of("imageId", imageId));
		AfterTransaction.onCommit(() -> storage.delete(key));
	}

}
