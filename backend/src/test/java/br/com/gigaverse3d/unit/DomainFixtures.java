package br.com.gigaverse3d.unit;

import java.math.BigDecimal;

import org.springframework.test.util.ReflectionTestUtils;

import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;

/** Objetos de domínio fictícios para testes unitários (sem banco nem Spring). */
public final class DomainFixtures {

	private DomainFixtures() {
	}

	public static Category category() {
		return new Category("miniaturas-teste", "Miniaturas de Teste", "Categoria fictícia");
	}

	public static Product product(String price, Product.Type type, Product.Status status) {
		Product p = new Product("dragao-teste", "GV-TESTE-001", "Dragão de Teste", "Descrição fictícia do dragão",
				category(), type, new BigDecimal(price));
		p.setStatus(status);
		return p;
	}

	public static Product activeReady(String price) {
		return product(price, Product.Type.READY, Product.Status.ACTIVE);
	}

	/** Atribui o id que o banco geraria (necessário em regras que comparam por id). */
	public static <T> T withId(T entity, long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}

}
