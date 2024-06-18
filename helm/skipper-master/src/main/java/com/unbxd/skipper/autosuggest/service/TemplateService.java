package com.unbxd.skipper.autosuggest.service;

import com.unbxd.skipper.autosuggest.exception.ValidationException;
import com.unbxd.skipper.autosuggest.model.Template;

import java.util.List;

public interface TemplateService {
    void addTemplate(Template template) throws ValidationException;
    List<Template> getTemplates();
    Template getTemplate(String templateId);
}
