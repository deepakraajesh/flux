package com.unbxd.skipper.autosuggest.service.impl;

import com.google.inject.Inject;
import com.unbxd.skipper.autosuggest.exception.ValidationException;
import com.unbxd.skipper.autosuggest.dao.TemplateDAO;
import com.unbxd.skipper.autosuggest.model.Template;
import com.unbxd.skipper.autosuggest.service.TemplateService;

import java.util.List;

public class TemplateServiceImpl implements TemplateService {

    private TemplateDAO templateDAO;

    @Inject
    public TemplateServiceImpl(TemplateDAO templateDAO) {
        this.templateDAO = templateDAO;
    }

    @Override
    public void addTemplate(Template template) throws ValidationException {
        if(template.getTemplateId() == null || template.getVertical() == null) {
            throw new ValidationException("templateId and vertical fields are mandatory");
        }
        templateDAO.add(template);
    }

    @Override
    public List<Template> getTemplates() {
        return templateDAO.getTemplates();
    }

    @Override
    public Template getTemplate(String templateId) {
        return templateDAO.getTemplate(templateId);
    }

}
