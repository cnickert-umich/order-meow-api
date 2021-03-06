package com.ordermeow.api.product;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String PRODUCT_NAME = "GARBAGE";
    private static final String PRODUCT_DESCRIPTION = "DESCRIPTION";
    private static final Long PRODUCT_ID = 1L;
    private static final String FILE_NAME = "File Name.png";
    private static final String IMAGE_TYPE = MediaType.IMAGE_JPEG_VALUE;
    private static final byte[] IMAGE_BYTES = new byte[]{0x01, 0x02, 0x03};

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Test
    void createProduct_happyPath() throws IOException {
        ProductEntity expected = new ProductEntity();
        MultipartFile file = mock(MultipartFile.class);

        expected.setProductName(PRODUCT_NAME);
        expected.setProductDescription(PRODUCT_DESCRIPTION);
        expected.setProductPrice(BigDecimal.valueOf(1.00));
        expected.setFileName(FILE_NAME);
        expected.setFileType(IMAGE_TYPE);
        expected.setProductImage(IMAGE_BYTES);

        when(file.getOriginalFilename()).thenReturn(FILE_NAME);
        when(file.getContentType()).thenReturn(IMAGE_TYPE);
        when(file.getBytes()).thenReturn(IMAGE_BYTES);
        when(productRepository.save(expected)).thenReturn(expected);

        ProductEntity actual = productService.createProduct(expected, file);

        Assertions.assertEquals(expected.getProductName(), actual.getProductName());
        Assertions.assertEquals(expected.getFileName(), actual.getFileName());
        Assertions.assertEquals(expected.getFileType(), actual.getFileType());
        Assertions.assertEquals(expected.getProductDescription(), actual.getProductDescription());
        Assertions.assertEquals(expected.getProductImage(), actual.getProductImage());
        Assertions.assertEquals(expected.getProductPrice(), actual.getProductPrice());
        verify(productRepository, Mockito.times(1)).save(expected);
    }

    @Test
    void createProduct_nullNameThrowsException() {
        ProductEntity badProduct = new ProductEntity();
        badProduct.setProductName(null);

        Assertions.assertThrows(ProductExceptions.BadProductName.class, () -> productService.createProduct(badProduct, null));
    }

    @Test
    void createProduct_emptyStringNameThrowsException() {
        ProductEntity badProduct = new ProductEntity();
        badProduct.setProductName("");

        Assertions.assertThrows(ProductExceptions.BadProductName.class, () -> productService.createProduct(badProduct, null));
    }

    @Test
    void createProduct_nullDescriptionThrowsException() {
        ProductEntity badProduct = new ProductEntity();
        badProduct.setProductName(PRODUCT_NAME);
        badProduct.setProductDescription(null);
        Assertions.assertThrows(ProductExceptions.BadProductDescription.class, () -> productService.createProduct(badProduct, null));
    }

    @Test
    void createProduct_emptyDescriptionThrowsException() {
        ProductEntity badProduct = new ProductEntity();
        badProduct.setProductName(PRODUCT_NAME);
        badProduct.setProductDescription("");

        Assertions.assertThrows(ProductExceptions.BadProductDescription.class, () -> productService.createProduct(badProduct, null));
    }

    @Test
    void createProduct_nullPriceThrowsException() {
        ProductEntity badProduct = new ProductEntity();
        badProduct.setProductName(PRODUCT_NAME);
        badProduct.setProductDescription(PRODUCT_DESCRIPTION);
        badProduct.setProductPrice(null);
        Assertions.assertThrows(ProductExceptions.BadProductPrice.class, () -> productService.createProduct(badProduct, null));
    }

    @Test
    void createProduct_priceEqualsZeroThrowsException() {
        ProductEntity badProduct = new ProductEntity();
        badProduct.setProductName(PRODUCT_NAME);
        badProduct.setProductDescription(PRODUCT_DESCRIPTION);
        badProduct.setProductPrice(BigDecimal.ZERO);
        Assertions.assertThrows(ProductExceptions.BadProductPrice.class, () -> productService.createProduct(badProduct, null));
    }

    @Test
    void createProduct_priceLessThanZeroThrowsException() {
        ProductEntity badProduct = new ProductEntity();
        badProduct.setProductName(PRODUCT_NAME);
        badProduct.setProductDescription(PRODUCT_DESCRIPTION);
        badProduct.setProductPrice(BigDecimal.valueOf(-1));
        Assertions.assertThrows(ProductExceptions.BadProductPrice.class, () -> productService.createProduct(badProduct, null));
    }

    @Test
    void createProduct_badFileNameThrowsException() {
        MultipartFile file = mock(MultipartFile.class);
        ProductEntity badProduct = new ProductEntity();
        badProduct.setProductName(PRODUCT_NAME);
        badProduct.setProductDescription(PRODUCT_DESCRIPTION);
        badProduct.setProductPrice(BigDecimal.valueOf(1.00));
        when(file.getOriginalFilename()).thenReturn("..");
        Assertions.assertThrows(ProductExceptions.InvalidFileException.class, () -> productService.createProduct(badProduct, file));
    }

    @Test
    void createProduct_ioExceptionReadingFileBytes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        ProductEntity badProduct = new ProductEntity();
        badProduct.setProductName(PRODUCT_NAME);
        badProduct.setProductDescription(PRODUCT_DESCRIPTION);
        badProduct.setProductPrice(BigDecimal.valueOf(1.00));
        when(file.getOriginalFilename()).thenReturn(FILE_NAME);
        when(file.getContentType()).thenReturn(IMAGE_TYPE);
        when(file.getBytes()).thenThrow(new IOException());

        Assertions.assertThrows(ProductExceptions.InvalidFileException.class, () -> productService.createProduct(badProduct, file));
    }

    @Test
    void getProduct_happyPath() {
        ProductEntity expected = new ProductEntity();
        expected.setProductId(PRODUCT_ID);
        expected.setProductName(PRODUCT_NAME);
        expected.setProductDescription(PRODUCT_DESCRIPTION);
        expected.setProductPrice(BigDecimal.ONE);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(expected));
        ProductEntity actual = productService.getProduct(PRODUCT_ID);

        Assertions.assertEquals(expected.getProductName(), actual.getProductName());
        Assertions.assertEquals(expected.getProductPrice(), actual.getProductPrice());
        Assertions.assertEquals(expected.getProductDescription(), actual.getProductDescription());
    }

    @Test
    void getProduct_notFound() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        Assertions.assertThrows(ProductExceptions.ProductNotFound.class, () -> productService.getProduct(PRODUCT_ID));
    }

    @Test
    void getAllProducts() {
        List<ProductEntity> expected = new ArrayList<>();
        int numProducts = (int) (Math.random() * 10);
        for (int i = 0; i < numProducts; i++) {
            ProductEntity product = new ProductEntity();
            product.setProductId((long) i);
            product.setProductDescription("Product " + i);
            expected.add(product);
        }

        when(productRepository.findAll()).thenReturn(expected);
        List<ProductEntity> actual = productService.getProducts();
        Assertions.assertIterableEquals(expected, actual);

    }

    @Test
    void deleteProduct_happyPath() {
        ProductEntity expected = new ProductEntity();
        expected.setProductId(PRODUCT_ID);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(expected));
        doNothing().when(productRepository).deleteById(PRODUCT_ID);

        productService.deleteProductById(PRODUCT_ID);
        verify(productRepository, times(1)).findById(PRODUCT_ID);
        verify(productRepository, times(1)).deleteById(PRODUCT_ID);
    }

    @Test
    void deleteProduct_productIdNotFound() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        Assertions.assertThrows(ProductExceptions.ProductNotFound.class, () -> productService.deleteProductById(PRODUCT_ID));

        verify(productRepository, times(0)).deleteById(PRODUCT_ID);
    }

    @Test
    void editProduct_productNotFound() {
        ProductEntity product = new ProductEntity();
        product.setProductId(PRODUCT_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        Assertions.assertThrows(ProductExceptions.ProductNotFound.class, () -> productService.editProduct(product, null));
        verify(productRepository, times(0)).save(product);
    }

    @Test
    void editProduct_allFieldsSuccess() throws Exception {
        ProductEntity product = new ProductEntity();
        ProductEntity expectedUpdatedProduct = new ProductEntity();

        product.setProductId(PRODUCT_ID);
        product.setProductName(PRODUCT_NAME);
        product.setProductDescription(PRODUCT_DESCRIPTION);
        product.setProductPrice(BigDecimal.ONE);
        product.setProductImage(IMAGE_BYTES);
        product.setFileType(IMAGE_TYPE);
        product.setFileName(FILE_NAME);

        // Updated product has new fields
        final String UPDATED = "UPDATED";

        MockMultipartFile file =
                new MockMultipartFile(
                        FILE_NAME + UPDATED,
                        FILE_NAME + UPDATED,
                        MediaType.IMAGE_GIF_VALUE,
                        "<<pdf data>>".getBytes(StandardCharsets.UTF_8));

        expectedUpdatedProduct.setProductId(PRODUCT_ID);
        expectedUpdatedProduct.setProductName(PRODUCT_NAME + UPDATED);
        expectedUpdatedProduct.setProductDescription(PRODUCT_DESCRIPTION + UPDATED);
        expectedUpdatedProduct.setProductPrice(BigDecimal.TEN);

        when(productRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(productRepository.save(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        ProductEntity actual = productService.editProduct(expectedUpdatedProduct, file);

        Assertions.assertEquals(expectedUpdatedProduct.getProductName(), actual.getProductName());
        Assertions.assertEquals(expectedUpdatedProduct.getProductDescription(), actual.getProductDescription());
        Assertions.assertEquals(expectedUpdatedProduct.getProductPrice().setScale(2, RoundingMode.CEILING), actual.getProductPrice());
        Assertions.assertEquals(file.getContentType(), actual.getFileType());
        Assertions.assertEquals(file.getBytes(), actual.getProductImage());
        Assertions.assertEquals(file.getName(), actual.getFileName());

        verify(productRepository, times(1)).save(Mockito.any());
    }
}
