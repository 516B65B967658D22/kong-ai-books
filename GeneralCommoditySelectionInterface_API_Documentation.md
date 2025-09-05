# 通用商品选择接口文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档标题 | 通用商品选择接口文档 |
| 接口名称 | GeneralCommoditySelectionInterface |
| 文档版本 | v1.0 |
| 创建日期 | 2024年12月 |
| 最后更新 | 2024年12月 |
| 文档状态 | 正式版 |

---

## 目录

1. [接口概述](#接口概述)
2. [接口方法列表](#接口方法列表)
3. [接口详细说明](#接口详细说明)
   - [3.1 查询热销商品列表-游客](#31-查询热销商品列表-游客)
   - [3.2 查询热销商品列表-登录用户](#32-查询热销商品列表-登录用户)
   - [3.3 查询商品分页列表-游客](#33-查询商品分页列表-游客)
   - [3.4 查询商品分页列表-登录用户](#34-查询商品分页列表-登录用户)
   - [3.5 查询商品详情-游客](#35-查询商品详情-游客)
   - [3.6 查询商品详情-登录用户](#36-查询商品详情-登录用户)
4. [数据类型说明](#数据类型说明)
5. [错误码说明](#错误码说明)
6. [附录](#附录)

---

## 接口概述

### 接口描述
通用商品选择接口（GeneralCommoditySelectionInterface）提供全面的商品选择功能，支持游客（公开访问）和登录用户两种访问模式。该接口包含热销商品查询、分页商品列表查询和商品详情查询等核心功能。

### 功能特性
- 支持游客和登录用户两种访问模式
- 提供热销商品推荐功能
- 支持多维度商品筛选和排序
- 提供完整的商品详情信息
- 支持分页查询，提升查询性能

### 技术规范
- **协议**: HTTP/HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8
- **请求方式**: POST
- **认证方式**: Token认证（登录用户接口）

---

## 接口方法列表

| 序号 | 方法名称 | 英文方法名 | 访问权限 | 功能描述 |
|------|----------|------------|----------|----------|
| 1 | 查询热销商品列表-游客 | `getHotCommoditysPublic` | 公开访问 | 获取热销商品列表，无需登录 |
| 2 | 查询热销商品列表-登录用户 | `getHotCommoditysAuth` | 需要登录 | 获取热销商品列表，需要登录 |
| 3 | 查询商品分页列表-游客 | `getCommodityPagePublic` | 公开访问 | 分页查询商品列表，无需登录 |
| 4 | 查询商品分页列表-登录用户 | `getCommodityPageAuth` | 需要登录 | 分页查询商品列表，需要登录 |
| 5 | 查询商品详情-游客 | `getCommodityDetailPublic` | 公开访问 | 查询商品详细信息，无需登录 |
| 6 | 查询商品详情-登录用户 | `getCommodityDetailAuth` | 需要登录 | 查询商品详细信息，需要登录 |

---

## 接口详细说明

### 3.1 查询热销商品列表-游客

#### 基本信息
- **方法名称**: 查询热销商品列表-游客
- **英文方法名**: `getHotCommoditysPublic`
- **访问权限**: 公开访问（无需认证）
- **请求方式**: POST
- **功能描述**: 获取指定店铺的热销商品列表，适用于游客用户

#### 请求参数

| 参数名称 | 数据类型 | 是否必填 | 参数描述 | 示例值 |
|----------|----------|----------|----------|--------|
| shopId | String | 是 | 店铺ID | "12345" |

#### 响应参数

| 参数名称 | 数据类型 | 参数描述 | 示例值 |
|----------|----------|----------|--------|
| commodityId | String | 商品ID | "PROD001" |
| picture | String | 商品图片URL | "https://example.com/product1.jpg" |
| productName | String | 商品名称 | "高级机油" |
| guidePrice | Decimal | 销售指导价 | 299.99 |
| salesOrganization | String | 销售组织 | "华北销售部" |

#### 请求示例
```json
{
  "shopId": "12345"
}
```

#### 响应示例
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "commodityId": "PROD001",
      "picture": "https://example.com/product1.jpg",
      "productName": "高级机油",
      "guidePrice": 299.99,
      "salesOrganization": "华北销售部"
    }
  ]
}
```

---

### 3.2 查询热销商品列表-登录用户

#### 基本信息
- **方法名称**: 查询热销商品列表-登录用户
- **英文方法名**: `getHotCommoditysAuth`
- **访问权限**: 需要登录（无需特殊授权）
- **请求方式**: POST
- **功能描述**: 获取指定店铺的热销商品列表，适用于已登录用户

#### 请求参数

| 参数名称 | 数据类型 | 是否必填 | 参数描述 | 示例值 |
|----------|----------|----------|----------|--------|
| shopId | String | 是 | 店铺ID | "12345" |

#### 响应参数

| 参数名称 | 数据类型 | 参数描述 | 示例值 |
|----------|----------|----------|--------|
| commodityId | String | 商品ID | "PROD001" |
| productPicture | String | 商品图片URL | "https://example.com/product1.jpg" |
| productName | String | 商品名称 | "高级机油" |
| guidePrice | Decimal | 销售指导价 | 299.99 |
| salesOrganization | String | 销售组织 | "华北销售部" |

#### 请求示例
```json
{
  "shopId": "12345"
}
```

#### 响应示例
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "commodityId": "PROD001",
      "productPicture": "https://example.com/product1.jpg",
      "productName": "高级机油",
      "guidePrice": 299.99,
      "salesOrganization": "华北销售部"
    }
  ]
}
```

---

### 3.3 查询商品分页列表-游客

#### 基本信息
- **方法名称**: 查询商品分页列表-游客
- **英文方法名**: `getCommodityPagePublic`
- **访问权限**: 公开访问（无需认证）
- **请求方式**: POST
- **功能描述**: 分页查询商品列表，支持多维度筛选和排序，适用于游客用户

#### 请求参数

| 参数名称 | 数据类型 | 是否必填 | 参数描述 | 示例值 |
|----------|----------|----------|----------|--------|
| current | Integer | 是 | 当前页码 | 1 |
| size | Integer | 是 | 每页数量 | 20 |
| sortType | Integer | 是 | 排序类型：0=综合排序，1=价格升序，2=价格降序，3=销量排序 | 0 |
| commodityLineId | String | 否 | 商品线ID（null表示全部） | "LINE001" |
| middleCategoryId | String | 否 | 中分类ID（null表示全部） | "MID001" |
| minCategoryId | String | 否 | 小分类ID（null表示全部） | "MIN001" |
| businessLineType | Integer | 否 | 业务线类型：0=润滑油，1=燃料等 | 0 |
| appIndustryId | String | 否 | 应用行业ID | "IND001" |

#### 响应参数

**分页信息**
| 参数名称 | 数据类型 | 参数描述 | 示例值 |
|----------|----------|----------|--------|
| pageNo | Integer | 当前页码 | 1 |
| pageSize | Integer | 每页数量 | 20 |
| totalPage | Integer | 总页数 | 5 |
| totalRows | Integer | 总记录数 | 100 |
| rows | Array | 商品数据列表 | - |

**商品信息（rows数组中的对象）**
| 参数名称 | 数据类型 | 参数描述 | 示例值 |
|----------|----------|----------|--------|
| commodityId | String | 商品ID | "PROD001" |
| picture | String | 商品图片 | "https://example.com/product1.jpg" |
| price | Decimal | 商品价格 | 299.99 |
| name | String | 商品名称 | "高级机油" |
| isFollow | Boolean | 关注状态 | false |
| feature | String | 商品特色 | "高性能" |
| materialCode | String | 物料代码 | "MAT001" |
| commodityCode | String | 商品代码 | "COM001" |
| guidePrice | Decimal | 销售指导价 | 299.99 |
| memberPrice | Decimal | 会员价格 | 279.99 |

#### 请求示例
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

#### 响应示例
```json
{
  "code": 200,
  "message": "成功",
  "pageNo": 1,
  "pageSize": 20,
  "totalPage": 5,
  "totalRows": 100,
  "rows": [
    {
      "commodityId": "PROD001",
      "picture": "https://example.com/product1.jpg",
      "price": 299.99,
      "name": "高级机油",
      "isFollow": false,
      "feature": "高性能",
      "materialCode": "MAT001",
      "commodityCode": "COM001",
      "guidePrice": 299.99,
      "memberPrice": 279.99
    }
  ]
}
```

---

### 3.4 查询商品分页列表-登录用户

#### 基本信息
- **方法名称**: 查询商品分页列表-登录用户
- **英文方法名**: `getCommodityPageAuth`
- **访问权限**: 需要登录（无需特殊授权）
- **请求方式**: POST
- **功能描述**: 分页查询商品列表，支持多维度筛选和排序，适用于已登录用户

#### 请求参数
与游客版本相同，请参考 [3.3 查询商品分页列表-游客](#33-查询商品分页列表-游客) 的请求参数。

#### 响应参数
与游客版本相同，请参考 [3.3 查询商品分页列表-游客](#33-查询商品分页列表-游客) 的响应参数。

---

### 3.5 查询商品详情-游客

#### 基本信息
- **方法名称**: 查询商品详情-游客
- **英文方法名**: `getCommodityDetailPublic`
- **访问权限**: 公开访问（无需认证）
- **请求方式**: POST
- **功能描述**: 查询指定商品的详细信息，适用于游客用户

#### 请求参数

| 参数名称 | 数据类型 | 是否必填 | 参数描述 | 示例值 |
|----------|----------|----------|----------|--------|
| commodityId | String | 是 | 商品ID | "PROD001" |

#### 响应参数

| 参数名称 | 数据类型 | 参数描述 | 示例值 |
|----------|----------|----------|--------|
| pictures | Array | 商品图片列表 | ["https://example.com/img1.jpg"] |
| price | Decimal | 商品价格 | 299.99 |
| memberPrice | Decimal | 会员价格 | 279.99 |
| guidePrice | Decimal | 销售指导价 | 299.99 |
| customerPrice | Decimal | 客户专享价 | 289.99 |
| name | String | 商品名称 | "高级机油" |
| description | String | 商品描述 | "现代发动机高性能机油" |
| materialCode | String | 物料代码 | "MAT001" |
| productLine | String | 产品线 | "高级润滑油" |
| brandName | String | 品牌名称 | "品牌A" |
| oilGrade | String | 油品等级 | "高级" |
| qualityLevel | String | 质量等级 | "API SN" |
| viscosityLevel | String | 粘度等级 | "5W-30" |
| boxSpec | String | 箱装规格 | "12x1L" |
| execStandard | String | 执行标准 | "API/SAE" |
| marketingActInfo | String | 营销活动信息 | "特别促销" |
| unit | String | 计量单位 | "升" |
| packageType | String | 包装类型 | "瓶装" |
| materialCategory | String | 物料大类 | "润滑油" |
| materialSubCategory | String | 物料中类 | "机油" |
| materialMinCategory | String | 物料小类 | "合成机油" |
| marketingDiscount | String | 营销折扣 | "9折优惠" |
| isFavorited | Boolean | 商品收藏状态 | false |
| isSoldOut | Boolean | 售罄状态 | false |
| detailDescription | String | 详细描述 | "详细产品规格说明..." |
| productCertFile | String | 产品认证文件 | "https://example.com/cert.pdf" |
| shopInfo | Object | 店铺信息 | {} |
| shopName | String | 店铺名称 | "高级油品店" |
| isFollowShop | Boolean | 店铺关注状态 | false |

#### 请求示例
```json
{
  "commodityId": "PROD001"
}
```

#### 响应示例
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "pictures": ["https://example.com/img1.jpg", "https://example.com/img2.jpg"],
    "price": 299.99,
    "memberPrice": 279.99,
    "guidePrice": 299.99,
    "customerPrice": 289.99,
    "name": "高级机油",
    "description": "现代发动机高性能机油",
    "materialCode": "MAT001",
    "productLine": "高级润滑油",
    "brandName": "品牌A",
    "oilGrade": "高级",
    "qualityLevel": "API SN",
    "viscosityLevel": "5W-30",
    "boxSpec": "12x1L",
    "execStandard": "API/SAE",
    "marketingActInfo": "特别促销",
    "unit": "升",
    "packageType": "瓶装",
    "materialCategory": "润滑油",
    "materialSubCategory": "机油",
    "materialMinCategory": "合成机油",
    "marketingDiscount": "9折优惠",
    "isFavorited": false,
    "isSoldOut": false,
    "detailDescription": "详细产品规格说明...",
    "productCertFile": "https://example.com/cert.pdf",
    "shopInfo": {},
    "shopName": "高级油品店",
    "isFollowShop": false
  }
}
```

---

### 3.6 查询商品详情-登录用户

#### 基本信息
- **方法名称**: 查询商品详情-登录用户
- **英文方法名**: `getCommodityDetailAuth`
- **访问权限**: 需要登录（无需特殊授权）
- **请求方式**: POST
- **功能描述**: 查询指定商品的详细信息，适用于已登录用户

#### 请求参数
与游客版本相同，请参考 [3.5 查询商品详情-游客](#35-查询商品详情-游客) 的请求参数。

#### 响应参数
与游客版本相同，请参考 [3.5 查询商品详情-游客](#35-查询商品详情-游客) 的响应参数。

---

## 数据类型说明

| 数据类型 | 描述 | 示例 |
|----------|------|------|
| String | 字符串类型 | "示例文本" |
| Integer | 整数类型 | 123 |
| Decimal | 小数类型 | 299.99 |
| Boolean | 布尔类型 | true, false |
| Array | 数组类型 | ["item1", "item2"] |
| Object | 对象类型 | {"key": "value"} |

---

## 错误码说明

| 错误码 | 错误信息 | 描述 | 解决方案 |
|--------|----------|------|----------|
| 200 | 成功 | 请求处理成功 | - |
| 400 | 请求参数错误 | 请求参数格式不正确或缺少必填参数 | 检查请求参数格式和完整性 |
| 401 | 未授权访问 | 访问需要登录的接口时未提供有效认证信息 | 请先登录获取有效token |
| 403 | 权限不足 | 当前用户没有访问该资源的权限 | 联系管理员获取相应权限 |
| 404 | 资源不存在 | 请求的资源不存在 | 检查资源ID是否正确 |
| 500 | 服务器内部错误 | 服务器处理请求时发生内部错误 | 联系技术支持 |

---

## 附录

### 排序类型说明
| 排序类型值 | 排序方式 | 描述 |
|------------|----------|------|
| 0 | 综合排序 | 根据综合算法进行排序 |
| 1 | 价格升序 | 按价格从低到高排序 |
| 2 | 价格降序 | 按价格从高到低排序 |
| 3 | 销量排序 | 按销量从高到低排序 |

### 业务线类型说明
| 业务线类型值 | 业务线名称 | 描述 |
|--------------|------------|------|
| 0 | 润滑油 | 润滑油相关产品 |
| 1 | 燃料 | 燃料相关产品 |

### 访问权限说明
- **公开访问**: 无需任何认证，所有用户均可访问
- **需要登录**: 需要用户登录并提供有效的认证token，但无需特殊授权

### 注意事项
1. 所有请求和响应均使用JSON格式
2. 所有字符串均使用UTF-8编码
3. 价格相关字段均为Decimal类型，保留两位小数
4. 布尔类型字段使用true/false表示
5. 数组类型字段在没有数据时返回空数组[]
6. 对象类型字段在没有数据时返回空对象{}
7. 分页查询时，页码从1开始计数

---

*文档版本: v1.0*  
*最后更新: 2024年12月*  
*接口名称: GeneralCommoditySelectionInterface*  
*维护团队: Kong AI Books 开发团队*