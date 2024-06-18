package com.unbxd.skipper.autosuggest.dao;

import com.unbxd.skipper.autosuggest.model.Template;

import java.util.List;


public interface TemplateDAO {
    void add(Template template);
    List<Template> getTemplates();
    Template getTemplate(String templateId);
}
