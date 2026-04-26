package com.example.patent.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.patent.config.CustomStyleHandler;
import com.example.patent.dto.*;
import com.example.patent.entity.PatentInfo;
import com.example.patent.entity.PatentInfoField;
import com.example.patent.entity.PatentSplitFieldType;
import com.example.patent.mapper.PatentInfoFieldMapper;
import com.example.patent.mapper.PatentInfoMapper;
import com.example.patent.service.PatentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatentServiceImpl implements PatentService {

    @Autowired
    private PatentInfoMapper patentInfoMapper;

    @Autowired
    private PatentInfoFieldMapper patentInfoFieldMapper;

    @Override
    @Transactional
    public void addPatent(PatentAddRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("专利名称不能为空");
        }

        String applicationNumber = request.getApplicationNumber();
        if (StringUtils.hasText(applicationNumber)) {
            LambdaQueryWrapper<PatentInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PatentInfo::getApplicationNumber, applicationNumber.trim());
            PatentInfo existingPatent = patentInfoMapper.selectOne(queryWrapper);
            if (existingPatent != null) {
                throw new IllegalArgumentException("申请号已存在: " + applicationNumber);
            }
        }

        PatentInfo patent = new PatentInfo();
        patent.setId(UUID.randomUUID().toString());
        patent.setTitle(request.getTitle().trim());
        patent.setSerialNumber(request.getSerialNumber());
        patent.setApplicationNumber(request.getApplicationNumber() == null ? UUID.randomUUID().toString() : request.getApplicationNumber().trim());
        patent.setApplicationDate(request.getApplicationDate());
        patent.setPatentType(request.getPatentType());
        patent.setLegalStatus(request.getLegalStatus());
        patent.setCollege(request.getCollege());
        patent.setCurrentAssignee(request.getCurrentAssignee());
        patent.setOriginalAssignee(request.getOriginalAssignee());
        patent.setApplicationYear(parseInteger(request.getApplicationYear()));
        patent.setPublicationDate(request.getPublicationDate());
        patent.setGrantDate(request.getGrantDate());
        patent.setGrantYear(parseInteger(request.getGrantYear()));
        patent.setIpcMainClass(request.getIpcMainClass());
        patent.setIpcMainClassInterpretation(request.getIpcMainClassInterpretation());
        patent.setPatentValue(parseMoneyValue(request.getPatentValue()));
        patent.setTechnicalValue(parseMoneyValue(request.getTechnicalValue()));
        patent.setMarketValue(parseMoneyValue(request.getMarketValue()));
        patent.setTechnicalProblem(request.getTechnicalProblem());
        patent.setTechnicalEffect(request.getTechnicalEffect());
        patent.setInventorCount(parseInteger(request.getInventorCount()));
        patent.setAgency(request.getAgency());
        patent.setCurrentAssigneeProvince(request.getCurrentAssigneeProvince());
        patent.setOriginalAssigneeProvince(request.getOriginalAssigneeProvince());
        patent.setOriginalAssigneeType(request.getOriginalAssigneeType());
        patent.setCurrentAssigneeType(request.getCurrentAssigneeType());
        patent.setStrategicIndustryClassification(request.getStrategicIndustryClassification());
        patent.setTechnicalSubjectClassification(request.getTechnicalSubjectClassification());
        patent.setExpiryDate(request.getExpiryDate());
        patent.setSimpleFamilyCitedPatents(parseInteger(request.getSimpleFamilyCitedPatents()));
        patent.setCitedPatents(parseInteger(request.getCitedPatents()));
        patent.setCitedIn5Years(parseInteger(request.getCitedIn5Years()));
        patent.setClaimsCount(parseInteger(request.getClaimsCount()));
        patent.setTransferEffectiveDate(request.getTransferEffectiveDate());
        patent.setLicenseType(request.getLicenseType());
        patent.setLicenseCount(parseInteger(request.getLicenseCount()));
        patent.setLicenseEffectiveDate(request.getLicenseEffectiveDate());
        patent.setTransferor(request.getTransferor());
        patent.setTransferee(request.getTransferee());

        patentInfoMapper.insert(patent);

        savePatentFields(
                patent.getId(),
                request.getInventors(),
                request.getTechnicalFields(),
                request.getIpcClassifications(),
                request.getCpcClassifications(),
                request.getTechnicalProblem(),
                request.getTechnicalEffect(),
                request.getTechnicalSubjectClassification(),
                request.getApplicationFieldClassification(),
                request.getIpcMainClassInterpretation(),
                request.getStrategicIndustryClassification()
        );
    }

    @Override
    @Transactional
    public void updatePatent(PatentAddRequest request) {
        if (!StringUtils.hasText(request.getId())) {
            throw new IllegalArgumentException("专利ID不能为空");
        }

        PatentInfo existingPatent = patentInfoMapper.selectById(request.getId());
        if (existingPatent == null) {
            throw new IllegalArgumentException("专利不存在");
        }

        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("专利名称不能为空");
        }

        String applicationNumber = request.getApplicationNumber();
        if (StringUtils.hasText(applicationNumber)) {
            LambdaQueryWrapper<PatentInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PatentInfo::getApplicationNumber, applicationNumber.trim());
            queryWrapper.ne(PatentInfo::getId, request.getId());
            PatentInfo duplicatePatent = patentInfoMapper.selectOne(queryWrapper);
            if (duplicatePatent != null) {
                throw new IllegalArgumentException("申请号已被其他专利使用: " + applicationNumber);
            }
        }

        existingPatent.setTitle(request.getTitle().trim());
        existingPatent.setSerialNumber(request.getSerialNumber());
        existingPatent.setApplicationNumber(request.getApplicationNumber());
        existingPatent.setApplicationDate(request.getApplicationDate());
        existingPatent.setPatentType(request.getPatentType());
        existingPatent.setLegalStatus(request.getLegalStatus());
        existingPatent.setCollege(request.getCollege());
        existingPatent.setCurrentAssignee(request.getCurrentAssignee());
        existingPatent.setOriginalAssignee(request.getOriginalAssignee());
        existingPatent.setApplicationYear(parseInteger(request.getApplicationYear()));
        existingPatent.setPublicationDate(request.getPublicationDate());
        existingPatent.setGrantDate(request.getGrantDate());
        existingPatent.setGrantYear(parseInteger(request.getGrantYear()));
        existingPatent.setIpcMainClass(request.getIpcMainClass());
        existingPatent.setIpcMainClassInterpretation(request.getIpcMainClassInterpretation());
        existingPatent.setTechnicalProblem(request.getTechnicalProblem());
        existingPatent.setTechnicalEffect(request.getTechnicalEffect());
        existingPatent.setInventorCount(parseInteger(request.getInventorCount()));
        existingPatent.setAgency(request.getAgency());
        existingPatent.setCurrentAssigneeProvince(request.getCurrentAssigneeProvince());
        existingPatent.setOriginalAssigneeProvince(request.getOriginalAssigneeProvince());
        existingPatent.setOriginalAssigneeType(request.getOriginalAssigneeType());
        existingPatent.setCurrentAssigneeType(request.getCurrentAssigneeType());
        existingPatent.setStrategicIndustryClassification(request.getStrategicIndustryClassification());
        existingPatent.setTechnicalSubjectClassification(request.getTechnicalSubjectClassification());
        existingPatent.setExpiryDate(request.getExpiryDate());
        existingPatent.setSimpleFamilyCitedPatents(parseInteger(request.getSimpleFamilyCitedPatents()));
        existingPatent.setCitedPatents(parseInteger(request.getCitedPatents()));
        existingPatent.setCitedIn5Years(parseInteger(request.getCitedIn5Years()));
        existingPatent.setClaimsCount(parseInteger(request.getClaimsCount()));
        existingPatent.setPatentValue(parseMoneyValue(request.getPatentValue()));
        existingPatent.setTechnicalValue(parseMoneyValue(request.getTechnicalValue()));
        existingPatent.setMarketValue(parseMoneyValue(request.getMarketValue()));
        existingPatent.setTransferEffectiveDate(request.getTransferEffectiveDate());
        existingPatent.setLicenseType(request.getLicenseType());
        existingPatent.setLicenseCount(parseInteger(request.getLicenseCount()));
        existingPatent.setLicenseEffectiveDate(request.getLicenseEffectiveDate());
        existingPatent.setTransferor(request.getTransferor());
        existingPatent.setTransferee(request.getTransferee());

        patentInfoMapper.updateById(existingPatent);

        LambdaQueryWrapper<PatentInfoField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.eq(PatentInfoField::getPatentId, request.getId());
        patentInfoFieldMapper.delete(fieldWrapper);

        savePatentFields(
                request.getId(),
                request.getInventors(),
                request.getTechnicalFields(),
                request.getIpcClassifications(),
                request.getCpcClassifications(),
                request.getTechnicalProblem(),
                request.getTechnicalEffect(),
                request.getTechnicalSubjectClassification(),
                request.getApplicationFieldClassification(),
                request.getIpcMainClassInterpretation(),
                request.getStrategicIndustryClassification()
        );
    }

    @Override
    @Transactional
    public void deletePatent(String id) {
        PatentInfo patent = patentInfoMapper.selectById(id);
        if (patent == null) {
            throw new IllegalArgumentException("专利不存在");
        }

        LambdaQueryWrapper<PatentInfoField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.eq(PatentInfoField::getPatentId, id);
        patentInfoFieldMapper.delete(fieldWrapper);

        patentInfoMapper.deleteById(id);
    }

    @Override
    public PageResponse<PatentListResponse> getPatentList(PatentQueryRequest request) {
        Page<PatentInfo> page = new Page<>(request.getPage(), request.getSize());

        LambdaQueryWrapper<PatentInfo> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getKeyword())) {
            List<String> splitFieldPatentIds = patentInfoFieldMapper.selectPatentIdsByAnyFieldLike(request.getKeyword());
            wrapper.and(w -> w
                    .like(PatentInfo::getTitle, request.getKeyword())
                    .or().like(PatentInfo::getApplicationNumber, request.getKeyword())
                    .or().like(PatentInfo::getCurrentAssignee, request.getKeyword())
                    .or(splitFieldPatentIds != null && !splitFieldPatentIds.isEmpty(), q -> q.in(PatentInfo::getId, splitFieldPatentIds)));
        }
        if (StringUtils.hasText(request.getTitle())) {
            wrapper.like(PatentInfo::getTitle, request.getTitle());
        }
        if (StringUtils.hasText(request.getApplicationNumber())) {
            wrapper.like(PatentInfo::getApplicationNumber, request.getApplicationNumber());
        }
        if (StringUtils.hasText(request.getPatentType())) {
            wrapper.eq(PatentInfo::getPatentType, request.getPatentType());
        }
        if (StringUtils.hasText(request.getLegalStatus())) {
            wrapper.eq(PatentInfo::getLegalStatus, request.getLegalStatus());
        }
        if (StringUtils.hasText(request.getApplicationYear())) {
            wrapper.eq(PatentInfo::getApplicationYear, request.getApplicationYear());
        }
        if (StringUtils.hasText(request.getCollege())) {
            wrapper.like(PatentInfo::getCollege, request.getCollege());
        }
        if (StringUtils.hasText(request.getIpcMainClassInterpretation())) {
            applyFieldTypeFilter(wrapper, PatentSplitFieldType.IPC_MAIN_CLASS_INTERPRETATION, request.getIpcMainClassInterpretation());
        }
        if (StringUtils.hasText(request.getInventors())) {
            List<String> patentIds = patentInfoFieldMapper.selectPatentIdsByInventor(request.getInventors());
            if (patentIds != null && !patentIds.isEmpty()) {
                wrapper.in(PatentInfo::getId, patentIds);
            } else {
                wrapper.eq(PatentInfo::getId, "NO_MATCH");
            }
        }
        if (StringUtils.hasText(request.getTechnicalFields())) {
            applyFieldTypeFilter(wrapper, PatentSplitFieldType.TECHNICAL_FIELD, request.getTechnicalFields());
        }
        if (StringUtils.hasText(request.getTechnicalProblem())) {
            applyFieldTypeFilter(wrapper, PatentSplitFieldType.TECHNICAL_PROBLEM, request.getTechnicalProblem());
        }
        if (StringUtils.hasText(request.getTechnicalEffect())) {
            applyFieldTypeFilter(wrapper, PatentSplitFieldType.TECHNICAL_EFFECT, request.getTechnicalEffect());
        }
        if (StringUtils.hasText(request.getIpcClassifications())) {
            applyFieldTypeFilter(wrapper, PatentSplitFieldType.IPC_CLASSIFICATION, request.getIpcClassifications());
        }
        if (StringUtils.hasText(request.getCpcClassifications())) {
            applyFieldTypeFilter(wrapper, PatentSplitFieldType.CPC_CLASSIFICATION, request.getCpcClassifications());
        }
        if (StringUtils.hasText(request.getApplicationFieldClassification())) {
            applyFieldTypeFilter(wrapper, PatentSplitFieldType.APPLICATION_FIELD_CLASSIFICATION, request.getApplicationFieldClassification());
        }
        if (StringUtils.hasText(request.getTechnicalSubjectClassification())) {
            applyFieldTypeFilter(wrapper, PatentSplitFieldType.TECHNICAL_SUBJECT_CLASSIFICATION, request.getTechnicalSubjectClassification());
        }
        if (StringUtils.hasText(request.getStrategicIndustryClassification())) {
            applyFieldTypeFilter(wrapper, PatentSplitFieldType.STRATEGIC_INDUSTRY_CLASSIFICATION, request.getStrategicIndustryClassification());
        }

        wrapper.orderByDesc(PatentInfo::getCreateTime).orderByDesc(PatentInfo::getId);

        Page<PatentInfo> result = patentInfoMapper.selectPage(page, wrapper);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<PatentListResponse> responseList = new ArrayList<>();

        for (PatentInfo patent : result.getRecords()) {
            PatentListResponse response = new PatentListResponse();
            response.setId(patent.getId());
            response.setSerialNumber(patent.getSerialNumber());
            response.setTitle(patent.getTitle());
            response.setCollege(patent.getCollege());
            response.setLegalStatus(patent.getLegalStatus());
            response.setPatentType(patent.getPatentType());
            response.setApplicationNumber(patent.getApplicationNumber());
            response.setCurrentAssignee(patent.getCurrentAssignee());
            response.setOriginalAssignee(patent.getOriginalAssignee());
            response.setInventorCount(patent.getInventorCount() != null ? patent.getInventorCount().toString() : null);
            response.setAgency(patent.getAgency());
            response.setCurrentAssigneeProvince(patent.getCurrentAssigneeProvince());
            response.setOriginalAssigneeProvince(patent.getOriginalAssigneeProvince());
            response.setOriginalAssigneeType(patent.getOriginalAssigneeType());
            response.setCurrentAssigneeType(patent.getCurrentAssigneeType());
            response.setApplicationYear(patent.getApplicationYear() != null ? patent.getApplicationYear().toString() : null);
            response.setGrantYear(patent.getGrantYear() != null ? patent.getGrantYear().toString() : null);
            response.setIpcMainClass(patent.getIpcMainClass());
            response.setIpcMainClassInterpretation(joinFieldValues(patent.getId(), PatentSplitFieldType.IPC_MAIN_CLASS_INTERPRETATION));
            response.setTechnicalProblem(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_PROBLEM));
            response.setTechnicalEffect(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_EFFECT));
            response.setStrategicIndustryClassification(joinFieldValues(patent.getId(), PatentSplitFieldType.STRATEGIC_INDUSTRY_CLASSIFICATION));
            response.setApplicationFieldClassification(joinFieldValues(patent.getId(), PatentSplitFieldType.APPLICATION_FIELD_CLASSIFICATION));
            response.setTechnicalSubjectClassification(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_SUBJECT_CLASSIFICATION));
            response.setCitedPatents(patent.getCitedPatents() != null ? patent.getCitedPatents().toString() : null);
            response.setCitedIn5Years(patent.getCitedIn5Years() != null ? patent.getCitedIn5Years().toString() : null);
            response.setClaimsCount(patent.getClaimsCount() != null ? patent.getClaimsCount().toString() : null);
            response.setPatentValue(patent.getPatentValue() != null ? patent.getPatentValue().toString() : null);
            response.setTechnicalValue(patent.getTechnicalValue() != null ? patent.getTechnicalValue().toString() : null);
            response.setMarketValue(patent.getMarketValue() != null ? patent.getMarketValue().toString() : null);
            response.setTransferor(patent.getTransferor());
            response.setTransferee(patent.getTransferee());
            response.setLicenseType(patent.getLicenseType());
            response.setLicenseCount(patent.getLicenseCount() != null ? patent.getLicenseCount().toString() : null);

            response.setApplicationDate(patent.getApplicationDate() != null ? sdf.format(patent.getApplicationDate()) : null);
            response.setGrantDate(patent.getGrantDate() != null ? sdf.format(patent.getGrantDate()) : null);
            response.setPublicationDate(patent.getPublicationDate() != null ? sdf.format(patent.getPublicationDate()) : null);
            response.setExpiryDate(patent.getExpiryDate() != null ? sdf.format(patent.getExpiryDate()) : null);
            response.setTransferEffectiveDate(patent.getTransferEffectiveDate() != null ? sdf.format(patent.getTransferEffectiveDate()) : null);
            response.setLicenseEffectiveDate(patent.getLicenseEffectiveDate() != null ? sdf.format(patent.getLicenseEffectiveDate()) : null);

            response.setInventors(joinFieldValues(patent.getId(), PatentSplitFieldType.INVENTOR));
            response.setTechnicalFields(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_FIELD));
            response.setIpcClassifications(joinFieldValues(patent.getId(), PatentSplitFieldType.IPC_CLASSIFICATION));
            response.setCpcClassifications(joinFieldValues(patent.getId(), PatentSplitFieldType.CPC_CLASSIFICATION));

            responseList.add(response);
        }

        return PageResponse.of(responseList, result.getTotal(), request.getPage(), request.getSize());
    }

    @Override
    public PatentListResponse getPatentDetail(String id) {
        PatentInfo patent = patentInfoMapper.selectById(id);
        if (patent == null) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        PatentListResponse response = new PatentListResponse();
        response.setId(patent.getId());
        response.setSerialNumber(patent.getSerialNumber());
        response.setTitle(patent.getTitle());
        response.setCollege(patent.getCollege());
        response.setLegalStatus(patent.getLegalStatus());
        response.setPatentType(patent.getPatentType());
        response.setApplicationNumber(patent.getApplicationNumber());
        response.setCurrentAssignee(patent.getCurrentAssignee());
        response.setOriginalAssignee(patent.getOriginalAssignee());
        response.setInventorCount(patent.getInventorCount() != null ? patent.getInventorCount().toString() : null);
        response.setAgency(patent.getAgency());
        response.setCurrentAssigneeProvince(patent.getCurrentAssigneeProvince());
        response.setOriginalAssigneeProvince(patent.getOriginalAssigneeProvince());
        response.setOriginalAssigneeType(patent.getOriginalAssigneeType());
        response.setCurrentAssigneeType(patent.getCurrentAssigneeType());
        response.setApplicationYear(patent.getApplicationYear() != null ? patent.getApplicationYear().toString() : null);
        response.setGrantYear(patent.getGrantYear() != null ? patent.getGrantYear().toString() : null);
        response.setIpcMainClass(patent.getIpcMainClass());
        response.setIpcMainClassInterpretation(joinFieldValues(patent.getId(), PatentSplitFieldType.IPC_MAIN_CLASS_INTERPRETATION));
        response.setTechnicalProblem(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_PROBLEM));
        response.setTechnicalEffect(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_EFFECT));
        response.setStrategicIndustryClassification(joinFieldValues(patent.getId(), PatentSplitFieldType.STRATEGIC_INDUSTRY_CLASSIFICATION));
        response.setApplicationFieldClassification(joinFieldValues(patent.getId(), PatentSplitFieldType.APPLICATION_FIELD_CLASSIFICATION));
        response.setTechnicalSubjectClassification(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_SUBJECT_CLASSIFICATION));
        response.setCitedPatents(patent.getCitedPatents() != null ? patent.getCitedPatents().toString() : null);
        response.setCitedIn5Years(patent.getCitedIn5Years() != null ? patent.getCitedIn5Years().toString() : null);
        response.setClaimsCount(patent.getClaimsCount() != null ? patent.getClaimsCount().toString() : null);
        response.setPatentValue(patent.getPatentValue() != null ? patent.getPatentValue().toString() : null);
        response.setTechnicalValue(patent.getTechnicalValue() != null ? patent.getTechnicalValue().toString() : null);
        response.setMarketValue(patent.getMarketValue() != null ? patent.getMarketValue().toString() : null);
        response.setTransferor(patent.getTransferor());
        response.setTransferee(patent.getTransferee());
        response.setLicenseType(patent.getLicenseType());
        response.setLicenseCount(patent.getLicenseCount() != null ? patent.getLicenseCount().toString() : null);

        response.setApplicationDate(patent.getApplicationDate() != null ? sdf.format(patent.getApplicationDate()) : null);
        response.setGrantDate(patent.getGrantDate() != null ? sdf.format(patent.getGrantDate()) : null);
        response.setPublicationDate(patent.getPublicationDate() != null ? sdf.format(patent.getPublicationDate()) : null);
        response.setExpiryDate(patent.getExpiryDate() != null ? sdf.format(patent.getExpiryDate()) : null);
        response.setTransferEffectiveDate(patent.getTransferEffectiveDate() != null ? sdf.format(patent.getTransferEffectiveDate()) : null);
        response.setLicenseEffectiveDate(patent.getLicenseEffectiveDate() != null ? sdf.format(patent.getLicenseEffectiveDate()) : null);

        response.setInventors(joinFieldValues(patent.getId(), PatentSplitFieldType.INVENTOR));
        response.setTechnicalFields(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_FIELD));
        response.setIpcClassifications(joinFieldValues(patent.getId(), PatentSplitFieldType.IPC_CLASSIFICATION));
        response.setCpcClassifications(joinFieldValues(patent.getId(), PatentSplitFieldType.CPC_CLASSIFICATION));

        return response;
    }

    @Override
    public List<Map<String, Object>> getStatistics(String field, Integer limit) {
        if (!StringUtils.hasText(field)) {
            throw new IllegalArgumentException("统计字段不能为空");
        }

        Integer effectiveLimit = limit != null ? limit : 10;

        if ("inventors".equals(field)) {
            return patentInfoFieldMapper.selectStatisticsByFieldType(PatentSplitFieldType.INVENTOR.code(), effectiveLimit);
        }

        List<String> simpleFields = Arrays.asList("college");
        String dbField = camelToUnderscore(field);

        if (simpleFields.contains(dbField)) {
            return patentInfoMapper.selectStatisticsByField(dbField, effectiveLimit);
        }

        Optional<PatentSplitFieldType> splitFieldType = PatentSplitFieldType.fromCode(dbField);
        if (splitFieldType.isPresent()) {
            return patentInfoFieldMapper.selectStatisticsByFieldType(splitFieldType.get().code(), effectiveLimit);
        }

        throw new IllegalArgumentException("不支持的字段: " + field);
    }

    @Override
    @Transactional
    public ImportResult importPatents(List<PatentImportData> dataList) {
        ImportResult result = new ImportResult();

        for (PatentImportData data : dataList) {
            result.setTotal(result.getTotal() + 1);
            try {
                String duplicate = savePatentFromImport(data);
                if (duplicate != null) {
                    result.setSkipped(result.getSkipped() + 1);
                    result.getDuplicates().add(duplicate);
                } else {
                    result.setSuccess(result.getSuccess() + 1);
                }
            } catch (Exception e) {
                result.setSkipped(result.getSkipped() + 1);
            }
        }

        return result;
    }

    private String savePatentFromImport(PatentImportData data) {
        if (!StringUtils.hasText(data.getTitle())) {
            throw new IllegalArgumentException("专利名称不能为空");
        }

        String applicationNumber = data.getApplicationNumber();
        if (StringUtils.hasText(applicationNumber)) {
            LambdaQueryWrapper<PatentInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PatentInfo::getApplicationNumber, applicationNumber.trim());
            PatentInfo existingPatent = patentInfoMapper.selectOne(queryWrapper);

            if (existingPatent != null) {
                return applicationNumber;
            }
        }

        PatentInfo patent = new PatentInfo();
        patent.setId(UUID.randomUUID().toString());
        patent.setSerialNumber(data.getSerialNumber() != null ? data.getSerialNumber() : data.getApplicationNumber());
        patent.setTitle(data.getTitle().trim());
        patent.setApplicationNumber(data.getApplicationNumber());
        patent.setPatentType(parsePatentType(data.getPatentType()));
        patent.setLegalStatus(parseLegalStatus(data.getLegalStatus()));
        patent.setCollege(data.getCollege());
        patent.setCurrentAssignee(data.getCurrentAssignee());
        patent.setOriginalAssignee(data.getOriginalAssignee());
        patent.setInventorCount(parseInteger(data.getInventorCount()));
        patent.setAgency(data.getAgency());
        patent.setCurrentAssigneeProvince(data.getCurrentAssigneeProvince());
        patent.setOriginalAssigneeProvince(data.getOriginalAssigneeProvince());
        patent.setOriginalAssigneeType(parseAssigneeType(data.getOriginalAssigneeType()));
        patent.setCurrentAssigneeType(parseAssigneeType(data.getCurrentAssigneeType()));
        patent.setApplicationYear(parseInteger(data.getApplicationYear()));
        patent.setGrantYear(parseInteger(data.getGrantYear()));
        patent.setIpcMainClass(data.getIpcMainClass());
        patent.setIpcMainClassInterpretation(data.getIpcMainClassInterpretation());
        patent.setTechnicalProblem(data.getTechnicalProblem());
        patent.setTechnicalEffect(data.getTechnicalEffect());
        patent.setStrategicIndustryClassification(data.getStrategicIndustryClassification());
        patent.setTechnicalSubjectClassification(data.getTechnicalSubjectClassification());
        patent.setSimpleFamilyCitedPatents(parseInteger(data.getSimpleFamilyCitedPatents()));
        patent.setCitedPatents(parseInteger(data.getCitedPatents()));
        patent.setCitedIn5Years(parseInteger(data.getCitedIn5Years()));
        patent.setClaimsCount(parseInteger(data.getClaimsCount()));
        patent.setPatentValue(parseMoneyValue(data.getPatentValue()));
        patent.setTechnicalValue(parseMoneyValue(data.getTechnicalValue()));
        patent.setMarketValue(parseMoneyValue(data.getMarketValue()));
        patent.setLicenseType(data.getLicenseType());
        patent.setLicenseCount(parseInteger(data.getLicenseCount()));
        patent.setTransferor(data.getTransferor());
        patent.setTransferee(data.getTransferee());

        patent.setApplicationDate(parseDate(data.getApplicationDate()));
        patent.setGrantDate(parseDate(data.getGrantDate()));
        patent.setPublicationDate(parseDate(data.getPublicationDate()));
        patent.setExpiryDate(parseDate(data.getExpiryDate()));
        patent.setTransferEffectiveDate(parseDate(data.getTransferEffectiveDate()));
        patent.setLicenseEffectiveDate(parseDate(data.getLicenseEffectiveDate()));

        patentInfoMapper.insert(patent);

        savePatentFields(
                patent.getId(),
                data.getInventors(),
                data.getTechnicalFields(),
                data.getIpcClassifications(),
                data.getCpcClassifications(),
                data.getTechnicalProblem(),
                data.getTechnicalEffect(),
                data.getTechnicalSubjectClassification(),
                data.getApplicationFieldClassification(),
                data.getIpcMainClassInterpretation(),
                data.getStrategicIndustryClassification()
        );

        return null;
    }

    private void savePatentFields(String patentId,
                                  String inventors,
                                  String technicalFields,
                                  String ipcClassifications,
                                  String cpcClassifications,
                                  String technicalProblem,
                                  String technicalEffect,
                                  String technicalSubjectClassification,
                                  String applicationFieldClassification,
                                  String ipcMainClassInterpretation,
                                  String strategicIndustryClassification) {
        if (StringUtils.hasText(inventors)) {
            saveFieldList(patentId, PatentSplitFieldType.INVENTOR, inventors);
        }
        if (StringUtils.hasText(technicalFields)) {
            saveFieldList(patentId, PatentSplitFieldType.TECHNICAL_FIELD, technicalFields);
        }
        if (StringUtils.hasText(ipcClassifications)) {
            saveFieldList(patentId, PatentSplitFieldType.IPC_CLASSIFICATION, ipcClassifications);
        }
        if (StringUtils.hasText(cpcClassifications)) {
            saveFieldList(patentId, PatentSplitFieldType.CPC_CLASSIFICATION, cpcClassifications);
        }
        if (StringUtils.hasText(technicalProblem)) {
            saveFieldList(patentId, PatentSplitFieldType.TECHNICAL_PROBLEM, technicalProblem);
        }
        if (StringUtils.hasText(technicalEffect)) {
            saveFieldList(patentId, PatentSplitFieldType.TECHNICAL_EFFECT, technicalEffect);
        }
        if (StringUtils.hasText(technicalSubjectClassification)) {
            saveFieldList(patentId, PatentSplitFieldType.TECHNICAL_SUBJECT_CLASSIFICATION, technicalSubjectClassification);
        }
        if (StringUtils.hasText(applicationFieldClassification)) {
            saveFieldList(patentId, PatentSplitFieldType.APPLICATION_FIELD_CLASSIFICATION, applicationFieldClassification);
        }
        if (StringUtils.hasText(ipcMainClassInterpretation)) {
            saveFieldList(patentId, PatentSplitFieldType.IPC_MAIN_CLASS_INTERPRETATION, ipcMainClassInterpretation);
        }
        if (StringUtils.hasText(strategicIndustryClassification)) {
            saveFieldList(patentId, PatentSplitFieldType.STRATEGIC_INDUSTRY_CLASSIFICATION, strategicIndustryClassification);
        }
    }

    private void saveFieldList(String patentId, PatentSplitFieldType fieldType, String values) {
        List<String> items = splitValues(values, fieldType.delimiterRegex());
        for (int i = 0; i < items.size(); i++) {
            String value = items.get(i);
            if (!value.isEmpty()) {
                PatentInfoField field = new PatentInfoField();
                field.setId(UUID.randomUUID().toString());
                field.setPatentId(patentId);
                field.setFieldType(fieldType.code());
                field.setFieldValue(value);
                field.setSeq(i + 1);
                patentInfoFieldMapper.insert(field);
            }
        }
    }

    private List<String> splitValues(String values, String delimiterRegex) {
        if (!StringUtils.hasText(values)) {
            return Collections.emptyList();
        }
        String normalized = values.replace('｜', '|').replace('；', ';');
        return Arrays.stream(normalized.split(delimiterRegex))
                .map(item -> item.replaceAll("[\\r\\n\\t\\p{C}]", " ").replaceAll("\\s+", " ").trim())
                .filter(item -> !item.isEmpty() && !"-".equals(item))
                .collect(Collectors.toList());
    }

    private Date parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr) || "-".equals(dateStr)) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        } catch (ParseException e) {
            try {
                return new SimpleDateFormat("yyyy/MM/dd").parse(dateStr);
            } catch (ParseException ignored) {
                return null;
            }
        }
    }

    private String parsePatentType(String type) {
        if (!StringUtils.hasText(type)) return null;
        type = type.trim();
        if (type.contains("发明") && !type.contains("实用")) return "发明专利";
        if (type.contains("实用新型")) return "实用新型";
        if (type.contains("外观")) return "外观设计";
        return type;
    }

    private String parseLegalStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        status = status.trim();
        if (status.contains("授权") || status.contains("有效")) return "已授权";
        if (status.contains("审查")) return "审查中";
        if (status.contains("公开")) return "已公开";
        if (status.contains("驳回") || status.contains("拒绝")) return "已驳回";
        if (status.contains("撤回") || status.contains("放弃")) return "已撤回";
        if (status.contains("失效")) return "已失效";
        return status;
    }

    private String parseAssigneeType(String type) {
        if (!StringUtils.hasText(type)) return null;
        type = type.trim();
        if (type.contains("院校") || type.contains("研究院") || type.contains("大学")) return "高校";
        if (type.contains("公司") || type.contains("企业")) return "企业";
        if (type.contains("个人")) return "个人";
        if (type.equals("university")) return "高校";
        if (type.equals("company")) return "企业";
        if (type.equals("individual")) return "个人";
        if (type.equals("other")) return "其他";
        return type;
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            String cleaned = value.trim().replaceAll("[,，\\s]", "");
            if (cleaned.isEmpty() || "-".equals(cleaned)) return null;
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseMoneyValue(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            String cleaned = value.trim();
            cleaned = cleaned.replaceAll("[$￥€£]", "");
            cleaned = cleaned.replaceAll("[,，\\s]", "");
            if (cleaned.isEmpty() || "-".equals(cleaned)) return null;
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String camelToUnderscore(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private String joinFieldValues(String patentId, PatentSplitFieldType fieldType) {
        List<String> values = patentInfoFieldMapper.selectFieldValuesByPatentIdAndType(patentId, fieldType.code());
        return values != null && !values.isEmpty() ? String.join(" | ", values) : null;
    }

    private void applyFieldTypeFilter(LambdaQueryWrapper<PatentInfo> wrapper, PatentSplitFieldType fieldType, String keyword) {
        List<String> patentIds = patentInfoFieldMapper.selectPatentIdsByFieldTypeLike(fieldType.code(), keyword);
        if (patentIds != null && !patentIds.isEmpty()) {
            wrapper.in(PatentInfo::getId, patentIds);
        } else {
            wrapper.eq(PatentInfo::getId, "NO_MATCH");
        }
    }

    private String formatMultiValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.replace("|", " | ");
    }

    @Override
    public void exportPatents(PatentQueryRequest request, HttpServletResponse response) {
        LambdaQueryWrapper<PatentInfo> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(PatentInfo::getTitle, request.getKeyword())
                    .or().like(PatentInfo::getApplicationNumber, request.getKeyword())
                    .or().like(PatentInfo::getCurrentAssignee, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getPatentType())) {
            wrapper.eq(PatentInfo::getPatentType, request.getPatentType());
        }
        if (StringUtils.hasText(request.getLegalStatus())) {
            wrapper.eq(PatentInfo::getLegalStatus, request.getLegalStatus());
        }
        if (StringUtils.hasText(request.getApplicationYear())) {
            wrapper.eq(PatentInfo::getApplicationYear, request.getApplicationYear());
        }

        wrapper.orderByDesc(PatentInfo::getCreateTime).orderByDesc(PatentInfo::getId);

        List<PatentInfo> patentList = patentInfoMapper.selectList(wrapper);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<PatentListResponse> exportDataList = new ArrayList<>();

        for (PatentInfo patent : patentList) {
            PatentListResponse data = new PatentListResponse();
            data.setSerialNumber(patent.getSerialNumber());
            data.setTitle(patent.getTitle());
            data.setCollege(patent.getCollege());
            data.setLegalStatus(patent.getLegalStatus());
            data.setPatentType(patent.getPatentType());
            data.setApplicationNumber(patent.getApplicationNumber());
            data.setCurrentAssignee(patent.getCurrentAssignee());
            data.setOriginalAssignee(patent.getOriginalAssignee());
            data.setInventorCount(patent.getInventorCount() != null ? patent.getInventorCount().toString() : null);
            data.setAgency(patent.getAgency());
            data.setCurrentAssigneeProvince(patent.getCurrentAssigneeProvince());
            data.setOriginalAssigneeProvince(patent.getOriginalAssigneeProvince());
            data.setOriginalAssigneeType(patent.getOriginalAssigneeType());
            data.setCurrentAssigneeType(patent.getCurrentAssigneeType());
            data.setApplicationYear(patent.getApplicationYear() != null ? patent.getApplicationYear().toString() : null);
            data.setGrantYear(patent.getGrantYear() != null ? patent.getGrantYear().toString() : null);
            data.setIpcMainClass(patent.getIpcMainClass());
            data.setIpcMainClassInterpretation(joinFieldValues(patent.getId(), PatentSplitFieldType.IPC_MAIN_CLASS_INTERPRETATION));
            data.setTechnicalProblem(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_PROBLEM));
            data.setTechnicalEffect(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_EFFECT));
            data.setStrategicIndustryClassification(joinFieldValues(patent.getId(), PatentSplitFieldType.STRATEGIC_INDUSTRY_CLASSIFICATION));
            data.setApplicationFieldClassification(joinFieldValues(patent.getId(), PatentSplitFieldType.APPLICATION_FIELD_CLASSIFICATION));
            data.setTechnicalSubjectClassification(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_SUBJECT_CLASSIFICATION));
            data.setCitedPatents(patent.getCitedPatents() != null ? patent.getCitedPatents().toString() : null);
            data.setCitedIn5Years(patent.getCitedIn5Years() != null ? patent.getCitedIn5Years().toString() : null);
            data.setClaimsCount(patent.getClaimsCount() != null ? patent.getClaimsCount().toString() : null);
            data.setPatentValue(patent.getPatentValue() != null ? patent.getPatentValue().toString() : null);
            data.setTechnicalValue(patent.getTechnicalValue() != null ? patent.getTechnicalValue().toString() : null);
            data.setMarketValue(patent.getMarketValue() != null ? patent.getMarketValue().toString() : null);
            data.setTransferor(patent.getTransferor());
            data.setTransferee(patent.getTransferee());
            data.setLicenseType(patent.getLicenseType());
            data.setLicenseCount(patent.getLicenseCount() != null ? patent.getLicenseCount().toString() : null);

            data.setApplicationDate(patent.getApplicationDate() != null ? sdf.format(patent.getApplicationDate()) : null);
            data.setGrantDate(patent.getGrantDate() != null ? sdf.format(patent.getGrantDate()) : null);
            data.setPublicationDate(patent.getPublicationDate() != null ? sdf.format(patent.getPublicationDate()) : null);
            data.setExpiryDate(patent.getExpiryDate() != null ? sdf.format(patent.getExpiryDate()) : null);
            data.setTransferEffectiveDate(patent.getTransferEffectiveDate() != null ? sdf.format(patent.getTransferEffectiveDate()) : null);
            data.setLicenseEffectiveDate(patent.getLicenseEffectiveDate() != null ? sdf.format(patent.getLicenseEffectiveDate()) : null);

            data.setInventors(joinFieldValues(patent.getId(), PatentSplitFieldType.INVENTOR));
            data.setTechnicalFields(joinFieldValues(patent.getId(), PatentSplitFieldType.TECHNICAL_FIELD));
            data.setIpcClassifications(joinFieldValues(patent.getId(), PatentSplitFieldType.IPC_CLASSIFICATION));
            data.setCpcClassifications(joinFieldValues(patent.getId(), PatentSplitFieldType.CPC_CLASSIFICATION));

            exportDataList.add(data);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("专利数据导出", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream(), PatentListResponse.class)
                    .registerWriteHandler(new CustomStyleHandler())
                    .sheet("专利数据")
                    .doWrite(exportDataList);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败: " + e.getMessage());
        }
    }

    @Override
    public List<WordCloudResponse> getWordCloud(WordCloudRequest request) {
        String dimension = StringUtils.hasText(request.getDimension()) ? request.getDimension() : "inventors";
        Integer limit = request.getLimit() != null ? request.getLimit() : 100;

        Map<String, Integer> wordCounts = new HashMap<>();

        switch (dimension) {
            case "title":
                wordCounts = getTitleWordCloud(request);
                break;
            case "technicalFields":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.TECHNICAL_FIELD.code());
                break;
            case "technicalProblem":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.TECHNICAL_PROBLEM.code());
                break;
            case "technicalEffect":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.TECHNICAL_EFFECT.code());
                break;
            case "inventors":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.INVENTOR.code());
                break;
            case "ipcClassifications":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.IPC_CLASSIFICATION.code());
                break;
            case "cpcClassifications":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.CPC_CLASSIFICATION.code());
                break;
            case "ipcMainClass":
                wordCounts = getIpcMainClassWordCloud(request);
                break;
            case "ipcMainClassInterpretation":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.IPC_MAIN_CLASS_INTERPRETATION.code());
                break;
            case "college":
                wordCounts = getCollegeWordCloud(request);
                break;
            case "applicationFieldClassification":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.APPLICATION_FIELD_CLASSIFICATION.code());
                break;
            case "technicalSubjectClassification":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.TECHNICAL_SUBJECT_CLASSIFICATION.code());
                break;
            case "strategicIndustryClassification":
                wordCounts = getFieldTypeWordCloud(request, PatentSplitFieldType.STRATEGIC_INDUSTRY_CLASSIFICATION.code());
                break;
            default:
                wordCounts = getTitleWordCloud(request);
        }

        return wordCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(e -> new WordCloudResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private Map<String, Integer> getTitleWordCloud(WordCloudRequest request) {
        LambdaQueryWrapper<PatentInfo> wrapper = buildQueryWrapper(request);
        List<PatentInfo> patents = patentInfoMapper.selectList(wrapper);

        Map<String, Integer> wordCounts = new HashMap<>();
        for (PatentInfo patent : patents) {
            if (patent.getTitle() != null) {
                String[] words = segmentText(patent.getTitle());
                for (String word : words) {
                    if (word.length() > 1) {
                        wordCounts.merge(word, 1, Integer::sum);
                    }
                }
            }
        }
        return wordCounts;
    }

    private Map<String, Integer> getFieldTypeWordCloud(WordCloudRequest request, String fieldType) {
        LambdaQueryWrapper<PatentInfo> wrapper = buildQueryWrapper(request);
        wrapper.select(PatentInfo::getId);
        List<PatentInfo> patents = patentInfoMapper.selectList(wrapper);

        Map<String, Integer> wordCounts = new HashMap<>();
        for (PatentInfo patent : patents) {
            List<String> values = patentInfoFieldMapper.selectFieldValuesByPatentIdAndType(patent.getId(), fieldType);
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    wordCounts.merge(value.trim(), 1, Integer::sum);
                }
            }
        }
        return wordCounts;
    }

    private Map<String, Integer> getIpcMainClassWordCloud(WordCloudRequest request) {
        LambdaQueryWrapper<PatentInfo> wrapper = buildQueryWrapper(request);
        wrapper.select(PatentInfo::getIpcMainClass);
        List<PatentInfo> patents = patentInfoMapper.selectList(wrapper);

        Map<String, Integer> wordCounts = new HashMap<>();
        for (PatentInfo patent : patents) {
            if (patent.getIpcMainClass() != null && !patent.getIpcMainClass().trim().isEmpty()) {
                wordCounts.merge(patent.getIpcMainClass().trim(), 1, Integer::sum);
            }
        }
        return wordCounts;
    }

    private Map<String, Integer> getCollegeWordCloud(WordCloudRequest request) {
        LambdaQueryWrapper<PatentInfo> wrapper = buildQueryWrapper(request);
        wrapper.select(PatentInfo::getCollege);
        List<PatentInfo> patents = patentInfoMapper.selectList(wrapper);

        Map<String, Integer> wordCounts = new HashMap<>();
        for (PatentInfo patent : patents) {
            if (patent.getCollege() != null && !patent.getCollege().trim().isEmpty()) {
                wordCounts.merge(patent.getCollege().trim(), 1, Integer::sum);
            }
        }
        return wordCounts;
    }

    private LambdaQueryWrapper<PatentInfo> buildQueryWrapper(WordCloudRequest request) {
        LambdaQueryWrapper<PatentInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getPatentType())) {
            wrapper.eq(PatentInfo::getPatentType, request.getPatentType());
        }
        if (StringUtils.hasText(request.getLegalStatus())) {
            wrapper.eq(PatentInfo::getLegalStatus, request.getLegalStatus());
        }
        if (StringUtils.hasText(request.getApplicationYear())) {
            wrapper.eq(PatentInfo::getApplicationYear, request.getApplicationYear());
        }
        return wrapper;
    }

    private String[] segmentText(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        text = text.replaceAll("[\\p{Punct}\\p{Space}]+", " ");
        return text.split("\\s+");
    }

    @Override
    public List<TrendResponse> getTrend(TrendRequest request) {
        String dimension = request.getDimension();
        Integer limit = request.getLimit() != null ? request.getLimit() : 10;

        String dbField = camelToUnderscore(dimension);

        if ("inventors".equals(dimension)) {
            return getFieldTypeTrend(request, PatentSplitFieldType.INVENTOR, limit);
        }

        Optional<PatentSplitFieldType> splitFieldType = PatentSplitFieldType.fromCode(dbField);
        if (splitFieldType.isPresent()) {
            return getFieldTypeTrend(request, splitFieldType.get(), limit);
        }

        if (!"college".equals(dbField)) {
            throw new IllegalArgumentException("不支持的字段: " + dimension);
        }

        return getPatentFieldTrend(request, dbField, limit);
    }

    private List<TrendResponse> getInventorTrend(TrendRequest request, Integer limit) {
        return getFieldTypeTrend(request, PatentSplitFieldType.INVENTOR, limit);
    }

    private List<TrendResponse> getFieldTypeTrend(TrendRequest request, PatentSplitFieldType fieldType, Integer limit) {
        LambdaQueryWrapper<PatentInfo> wrapper = buildTrendQueryWrapper(request);
        wrapper.select(PatentInfo::getId, PatentInfo::getApplicationYear);
        wrapper.isNotNull(PatentInfo::getApplicationYear);
        wrapper.ne(PatentInfo::getApplicationYear, "");
        List<PatentInfo> patents = patentInfoMapper.selectList(wrapper);

        Map<String, Map<String, Integer>> yearFieldCount = new TreeMap<>();

        for (PatentInfo patent : patents) {
            String year = patent.getApplicationYear() != null ? patent.getApplicationYear().toString() : null;
            if (year == null || year.isEmpty()) continue;

            List<String> values = patentInfoFieldMapper.selectFieldValuesByPatentIdAndType(patent.getId(), fieldType.code());
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    yearFieldCount.computeIfAbsent(year, k -> new HashMap<>())
                            .merge(value.trim(), 1, Integer::sum);
                }
            }
        }

        List<TrendResponse> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> yearEntry : yearFieldCount.entrySet()) {
            String year = yearEntry.getKey();
            Map<String, Integer> fieldCounts = yearEntry.getValue();

            List<Map.Entry<String, Integer>> sortedFields = fieldCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            for (Map.Entry<String, Integer> entry : sortedFields) {
                result.add(new TrendResponse(year, entry.getKey(), entry.getValue()));
            }
        }

        return result;
    }

    private List<TrendResponse> getPatentFieldTrend(TrendRequest request, String dbField, Integer limit) {
        LambdaQueryWrapper<PatentInfo> wrapper = buildTrendQueryWrapper(request);
        wrapper.select(PatentInfo::getApplicationYear);
        wrapper.isNotNull(PatentInfo::getApplicationYear);
        wrapper.ne(PatentInfo::getApplicationYear, "");
        List<PatentInfo> patents = patentInfoMapper.selectList(wrapper);

        Map<String, Map<String, Integer>> yearFieldCount = new TreeMap<>();

        for (PatentInfo patent : patents) {
            String year = patent.getApplicationYear() != null ? patent.getApplicationYear().toString() : null;
            if (year == null || year.isEmpty()) continue;

            String fieldValue = getFieldValue(patent, dbField);
            if (fieldValue == null || fieldValue.isEmpty()) continue;

            String[] parts = fieldValue.split("[|,]");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    yearFieldCount.computeIfAbsent(year, k -> new HashMap<>())
                            .merge(trimmed, 1, Integer::sum);
                }
            }
        }

        List<TrendResponse> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> yearEntry : yearFieldCount.entrySet()) {
            String year = yearEntry.getKey();
            Map<String, Integer> fieldCounts = yearEntry.getValue();

            List<Map.Entry<String, Integer>> sortedFields = fieldCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            for (Map.Entry<String, Integer> entry : sortedFields) {
                result.add(new TrendResponse(year, entry.getKey(), entry.getValue()));
            }
        }

        return result;
    }

    private String getFieldValue(PatentInfo patent, String dbField) {
        switch (dbField) {
            case "college":
                return patent.getCollege();
            default:
                return null;
        }
    }

    private LambdaQueryWrapper<PatentInfo> buildTrendQueryWrapper(TrendRequest request) {
        LambdaQueryWrapper<PatentInfo> wrapper = new LambdaQueryWrapper<>();
        
        String startYear = request.getStartYear();
        String endYear = request.getEndYear();
        
        if (!StringUtils.hasText(startYear) && !StringUtils.hasText(endYear)) {
            int currentYear = java.time.Year.now().getValue();
            startYear = String.valueOf(currentYear - 9);
        }
        
        if (StringUtils.hasText(startYear)) {
            wrapper.ge(PatentInfo::getApplicationYear, startYear);
        }
        if (StringUtils.hasText(endYear)) {
            wrapper.le(PatentInfo::getApplicationYear, endYear);
        }
        if (StringUtils.hasText(request.getPatentType())) {
            wrapper.eq(PatentInfo::getPatentType, request.getPatentType());
        }
        if (StringUtils.hasText(request.getLegalStatus())) {
            wrapper.eq(PatentInfo::getLegalStatus, request.getLegalStatus());
        }
        return wrapper;
    }
}
