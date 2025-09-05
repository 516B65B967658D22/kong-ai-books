# General Commodity Selection Interface API Documentation

## Table of Contents
1. [Interface Overview](#interface-overview)
2. [API Methods](#api-methods)
   - [Hot Products Query - Public](#1-hot-products-query---public)
   - [Hot Products Query - Authenticated](#2-hot-products-query---authenticated)
   - [Product Page Query - Public](#3-product-page-query---public)
   - [Product Page Query - Authenticated](#4-product-page-query---authenticated)
   - [Product Detail Query - Public](#5-product-detail-query---public)
   - [Product Detail Query - Authenticated](#6-product-detail-query---authenticated)

---

## Interface Overview

**Interface Name:** GeneralCommoditySelectionInterface

This interface provides comprehensive commodity selection functionality, supporting both public (guest) and authenticated user access modes. It includes methods for querying hot-selling products, paginated product lists, and detailed product information.

---

## API Methods

### 1. Hot Products Query - Public

**Method Name:** 查询热销商品列表-游客 (Query Hot Products List - Guest)  
**English Method Name:** `getHotCommoditysPublic`  
**Access Level:** Public (No authentication required)

#### Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| shopId | String | Yes | Shop ID |

#### Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| commodityId | String | Product ID |
| picture | String | Product image URL |
| productName | String | Product name |
| guidePrice | Decimal | Sales guide price |
| salesOrganization | String | Sales organization |

#### Example Request
```json
{
  "shopId": "12345"
}
```

#### Example Response
```json
{
  "data": [
    {
      "commodityId": "PROD001",
      "picture": "https://example.com/product1.jpg",
      "productName": "Premium Motor Oil",
      "guidePrice": 299.99,
      "salesOrganization": "North Sales Division"
    }
  ]
}
```

---

### 2. Hot Products Query - Authenticated

**Method Name:** 查询热销商品列表-登录不鉴权 (Query Hot Products List - Authenticated No Authorization)  
**English Method Name:** `getHotCommoditysAuth`  
**Access Level:** Authenticated (Login required, no specific authorization)

#### Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| shopId | String | Yes | Shop ID |

#### Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| commodityId | String | Product ID |
| productPicture | String | Product image URL |
| productName | String | Product name |
| guidePrice | Decimal | Sales guide price |
| salesOrganization | String | Sales organization |

#### Example Request
```json
{
  "shopId": "12345"
}
```

#### Example Response
```json
{
  "data": [
    {
      "commodityId": "PROD001",
      "productPicture": "https://example.com/product1.jpg",
      "productName": "Premium Motor Oil",
      "guidePrice": 299.99,
      "salesOrganization": "North Sales Division"
    }
  ]
}
```

---

### 3. Product Page Query - Public

**Method Name:** 查询商品分页列表-游客 (Query Product Page List - Guest)  
**English Method Name:** `getCommodityPagePublic`  
**Access Level:** Public (No authentication required)

#### Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| current | Integer | Yes | Current page number |
| size | Integer | Yes | Number of items per page |
| sortType | Integer | Yes | Sort type: 0=Comprehensive, 1=Price Ascending, 2=Price Descending, 3=Sales Volume |
| commodityLineId | String | No | Product line ID (null for all) |
| middleCategoryId | String | No | Middle category ID (null for all) |
| minCategoryId | String | No | Sub category ID (null for all) |
| businessLineType | Integer | No | Business line type: 0=Lubricant, 1=Fuel, etc. |
| appIndustryId | String | No | Application industry ID |

#### Output Parameters

**Pagination Information:**
| Parameter | Type | Description |
|-----------|------|-------------|
| pageNo | Integer | Current page number |
| pageSize | Integer | Items per page |
| totalPage | Integer | Total pages |
| totalRows | Integer | Total rows |
| rows | Array | Product data array |

**Product Information (within rows):**
| Parameter | Type | Description |
|-----------|------|-------------|
| commodityId | String | Product ID |
| picture | String | Product image |
| price | Decimal | Product price |
| name | String | Product name |
| isFollow | Boolean | Follow status |

**New Product List Compatibility Fields:**
| Parameter | Type | Description |
|-----------|------|-------------|
| feature | String | Product features |

**VIC Exclusive Trading Hall Compatibility Fields:**
| Parameter | Type | Description |
|-----------|------|-------------|
| materialCode | String | Material code |
| commodityCode | String | Commodity code |
| guidePrice | Decimal | Sales guide price |
| memberPrice | Decimal | Member price |

#### Example Request
```json
{
  "current": 1,
  "size": 20,
  "sortType": 0,
  "commodityLineId": "LINE001",
  "middleCategoryId": null,
  "minCategoryId": null,
  "businessLineType": 0,
  "appIndustryId": "IND001"
}
```

#### Example Response
```json
{
  "pageNo": 1,
  "pageSize": 20,
  "totalPage": 5,
  "totalRows": 100,
  "rows": [
    {
      "commodityId": "PROD001",
      "picture": "https://example.com/product1.jpg",
      "price": 299.99,
      "name": "Premium Motor Oil",
      "isFollow": false,
      "feature": "High Performance",
      "materialCode": "MAT001",
      "commodityCode": "COM001",
      "guidePrice": 299.99,
      "memberPrice": 279.99
    }
  ]
}
```

---

### 4. Product Page Query - Authenticated

**Method Name:** 查询商品分页列表-登录不鉴权 (Query Product Page List - Authenticated No Authorization)  
**English Method Name:** `getCommodityPageAuth`  
**Access Level:** Authenticated (Login required, no specific authorization)

#### Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| current | Integer | Yes | Current page number |
| size | Integer | Yes | Number of items per page |
| sortType | Integer | Yes | Sort type: 0=Comprehensive, 1=Price Ascending, 2=Price Descending, 3=Sales Volume |
| commodityLineId | String | No | Product line ID (null for all) |
| middleCategoryId | String | No | Middle category ID (null for all) |
| minCategoryId | String | No | Sub category ID (null for all) |
| businessLineType | Integer | No | Business line type: 0=Lubricant, 1=Fuel, etc. |
| appIndustryId | String | No | Application industry ID |

#### Output Parameters

**Pagination Information:**
| Parameter | Type | Description |
|-----------|------|-------------|
| pageNo | Integer | Current page number |
| pageSize | Integer | Items per page |
| totalPage | Integer | Total pages |
| totalRows | Integer | Total rows |
| rows | Array | Product data array |

**Product Information (within rows):**
| Parameter | Type | Description |
|-----------|------|-------------|
| commodityId | String | Product ID |
| picture | String | Product image |
| price | Decimal | Product price |
| name | String | Product name |
| isFollow | Boolean | Follow status |

**New Product List Compatibility Fields:**
| Parameter | Type | Description |
|-----------|------|-------------|
| feature | String | Product features |

**VIC Exclusive Trading Hall Compatibility Fields:**
| Parameter | Type | Description |
|-----------|------|-------------|
| materialCode | String | Material code |
| commodityCode | String | Commodity code |
| guidePrice | Decimal | Sales guide price |
| memberPrice | Decimal | Member price |

---

### 5. Product Detail Query - Public

**Method Name:** 查询商品详情-游客 (Query Product Detail - Guest)  
**English Method Name:** `getCommodityDetailPublic`  
**Access Level:** Public (No authentication required)

#### Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| commodityId | String | Yes | Product ID |

#### Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| pictures | Array | Product image list |
| price | Decimal | Product price |
| memberPrice | Decimal | Member price |
| guidePrice | Decimal | Sales guide price |
| customerPrice | Decimal | Customer exclusive price |
| name | String | Product name |
| description | String | Product description |
| materialCode | String | Material code |
| productLine | String | Product line |
| brandName | String | Brand name |
| oilGrade | String | Oil grade |
| qualityLevel | String | Quality level |
| viscosityLevel | String | Viscosity level |
| boxSpec | String | Box specification |
| execStandard | String | Execution standard |
| marketingActInfo | String | Marketing activity information |
| unit | String | Unit of measurement |
| packageType | String | Package type |
| materialCategory | String | Material category |
| materialSubCategory | String | Material sub-category |
| materialMinCategory | String | Material min-category |
| marketingDiscount | String | Marketing discount |
| isFavorited | Boolean | Product favorite status |
| isSoldOut | Boolean | Sold out status |
| detailDescription | String | Detail description |
| productCertFile | String | Product certification file |
| shopInfo | Object | Shop information |
| shopName | String | Shop name |
| isFollowShop | Boolean | Shop follow status |

#### Example Request
```json
{
  "commodityId": "PROD001"
}
```

#### Example Response
```json
{
  "pictures": ["https://example.com/img1.jpg", "https://example.com/img2.jpg"],
  "price": 299.99,
  "memberPrice": 279.99,
  "guidePrice": 299.99,
  "customerPrice": 289.99,
  "name": "Premium Motor Oil",
  "description": "High-performance motor oil for modern engines",
  "materialCode": "MAT001",
  "productLine": "Premium Lubricants",
  "brandName": "Brand A",
  "oilGrade": "Premium",
  "qualityLevel": "API SN",
  "viscosityLevel": "5W-30",
  "boxSpec": "12x1L",
  "execStandard": "API/SAE",
  "marketingActInfo": "Special promotion",
  "unit": "Liter",
  "packageType": "Bottle",
  "materialCategory": "Lubricants",
  "materialSubCategory": "Motor Oil",
  "materialMinCategory": "Synthetic Oil",
  "marketingDiscount": "10% off",
  "isFavorited": false,
  "isSoldOut": false,
  "detailDescription": "Detailed product specifications...",
  "productCertFile": "https://example.com/cert.pdf",
  "shopInfo": {},
  "shopName": "Premium Oil Shop",
  "isFollowShop": false
}
```

---

### 6. Product Detail Query - Authenticated

**Method Name:** 查询商品详情-登录不鉴权 (Query Product Detail - Authenticated No Authorization)  
**English Method Name:** `getCommodityDetailAuth`  
**Access Level:** Authenticated (Login required, no specific authorization)

#### Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| commodityId | String | Yes | Product ID |

#### Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| pictures | Array | Product image list |
| price | Decimal | Product price |
| memberPrice | Decimal | Member price |
| guidePrice | Decimal | Sales guide price |
| customerPrice | Decimal | Customer exclusive price |
| name | String | Product name |
| description | String | Product description |
| materialCode | String | Material code |
| productLine | String | Product line |
| brandName | String | Brand name |
| oilGrade | String | Oil grade |
| qualityLevel | String | Quality level |
| viscosityLevel | String | Viscosity level |
| boxSpec | String | Box specification |
| execStandard | String | Execution standard |
| marketingActInfo | String | Marketing activity information |
| unit | String | Unit of measurement |
| packageType | String | Package type |
| materialCategory | String | Material category |
| materialSubCategory | String | Material sub-category |
| materialMinCategory | String | Material min-category |
| marketingDiscount | String | Marketing discount |
| isFavorited | Boolean | Product favorite status |
| isSoldOut | Boolean | Sold out status |
| detailDescription | String | Detail description |
| productCertFile | String | Product certification file |
| shopInfo | Object | Shop information |
| shopName | String | Shop name |
| isFollowShop | Boolean | Shop follow status |

---

## Notes

1. **Authentication Levels:**
   - **Public:** No authentication required, accessible to all users
   - **Authenticated:** Login required but no specific authorization needed

2. **Data Types:**
   - **String:** Text data
   - **Integer:** Whole numbers
   - **Decimal:** Numbers with decimal points
   - **Boolean:** true/false values
   - **Array:** List of items
   - **Object:** Complex data structure

3. **Sort Types:**
   - 0: Comprehensive sorting
   - 1: Price ascending
   - 2: Price descending
   - 3: Sales volume

4. **Business Line Types:**
   - 0: Lubricant
   - 1: Fuel
   - Additional types may be available

---

*Document Version: 1.0*  
*Last Updated: December 2024*  
*Interface: GeneralCommoditySelectionInterface*