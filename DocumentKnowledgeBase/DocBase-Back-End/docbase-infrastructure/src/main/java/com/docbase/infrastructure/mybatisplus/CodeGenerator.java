package com.docbase.infrastructure.mybatisplus;

import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig.Builder;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.builder.Entity;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.querys.MySqlQuery;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import com.baomidou.mybatisplus.generator.fill.Property;
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;
import lombok.Data;

/**
 * MyBatis-Plus code generator utility.
 *
 * <p>This class is kept compile-safe for the upgraded stack and can be refined
 * later if code generation is needed again.</p>
 */
@Data
@lombok.Builder
public class CodeGenerator {

    private String author;
    private String module;
    private String tableName;
    private String databaseUrl;
    private String username;
    private String password;
    private String parentPackage;
    private Boolean isExtendsFromBaseEntity;

    public static void main(String[] args) {
        CodeGenerator generator = CodeGenerator.builder()
            .databaseUrl("jdbc:mysql://localhost:33067/docbase_knowledge")
            .username("root")
            .password("12345")
            .author("valarchie")
            .module("/docbase-orm/target/generated-code")
            .parentPackage("com.docbase")
            .tableName("sys_menu")
            .isExtendsFromBaseEntity(true)
            .build();

        generator.generateCode();
    }

    public void generateCode() {
        FastAutoGenerator generator = FastAutoGenerator.create(
            new Builder(databaseUrl, username, password)
                .dbQuery(new MySqlQuery())
                .typeConvert(new MySqlTypeConvert())
                .keyWordsHandler(new MySqlKeyWordsHandler()));

        globalConfig(generator);
        packageConfig(generator);
        injectionConfig(generator);
        strategyConfig(generator);
        generator.templateEngine(new VelocityTemplateEngine());
        generator.execute();
    }

    private void globalConfig(FastAutoGenerator generator) {
        generator.globalConfig(builder -> builder
            .outputDir(System.getProperty("user.dir") + module + "/src/main/java")
            .dateType(DateType.ONLY_DATE)
            .author(author)
            .enableSwagger()
            .commentDate("yyyy-MM-dd"));
    }

    private void packageConfig(FastAutoGenerator generator) {
        generator.packageConfig(builder -> builder
            .parent(parentPackage)
            .moduleName("orm")
            .entity("entity")
            .service("service")
            .serviceImpl("service.impl")
            .mapper("mapper")
            .xml("mapper.xml")
            .controller("controller"));
    }

    @SuppressWarnings("unused")
    private void templateConfig(FastAutoGenerator generator) {
        generator.templateConfig(builder -> builder
            .disable(TemplateType.ENTITY)
            .entity("/templates/entity.java")
            .service("/templates/service.java")
            .serviceImpl("/templates/serviceImpl.java")
            .mapper("/templates/mapper.java")
            .controller("/templates/controller.java"));
    }

    private void injectionConfig(FastAutoGenerator generator) {
        generator.injectionConfig(builder -> builder
            .beforeOutputFile((tableInfo, objectMap) ->
                System.out.println("tableInfo: " + tableInfo.getEntityName() + " objectMap: " + objectMap.size())));
    }

    private void strategyConfig(FastAutoGenerator generator) {
        generator.strategyConfig(builder -> {
            builder.enableCapitalMode()
                .enableSkipView()
                .disableSqlFilter()
                .addInclude(tableName);

            entityConfig(builder);
            controllerConfig(builder);
            serviceConfig(builder);
            mapperConfig(builder);
        });
    }

    private void entityConfig(StrategyConfig.Builder builder) {
        Entity.Builder entityBuilder = builder.entityBuilder();

        entityBuilder.enableLombok()
            .enableTableFieldAnnotation()
            .enableActiveRecord()
            .logicDeleteColumnName("deleted")
            .naming(NamingStrategy.underline_to_camel)
            .columnNaming(NamingStrategy.underline_to_camel)
            .addTableFills(new Column("create_time", FieldFill.INSERT))
            .addTableFills(new Column("creator_id", FieldFill.INSERT))
            .addTableFills(new Property("updateTime", FieldFill.INSERT_UPDATE))
            .addTableFills(new Property("updaterId", FieldFill.INSERT_UPDATE))
            .idType(IdType.AUTO)
            .formatFileName("%sEntity");

        if (Boolean.TRUE.equals(isExtendsFromBaseEntity)) {
            entityBuilder
                .superClass(BaseEntity.class)
                .addSuperEntityColumns("creator_id", "create_time", "creator_name", "updater_id", "update_time",
                    "updater_name", "deleted");
        }
    }

    private void controllerConfig(StrategyConfig.Builder builder) {
        builder.controllerBuilder()
            .superClass(BaseController.class)
            .enableHyphenStyle()
            .enableRestStyle()
            .formatFileName("%sController");
    }

    private void serviceConfig(StrategyConfig.Builder builder) {
        builder.serviceBuilder()
            .formatServiceFileName("%sService")
            .formatServiceImplFileName("%sServiceImpl");
    }

    private void mapperConfig(StrategyConfig.Builder builder) {
        builder.mapperBuilder()
            .formatMapperFileName("%sMapper")
            .formatXmlFileName("%sMapper");
    }
}
