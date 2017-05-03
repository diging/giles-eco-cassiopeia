package edu.asu.diging.gilesecosystem.cassiopeia.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.asu.diging.gilesecosystem.cassiopeia.core.properties.Properties;
import edu.asu.diging.gilesecosystem.cassiopeia.web.pages.SystemConfigPage;
import edu.asu.diging.gilesecosystem.cassiopeia.web.validators.SystemConfigValidator;
import edu.asu.diging.gilesecosystem.septemberutil.properties.MessageType;
import edu.asu.diging.gilesecosystem.septemberutil.service.ISystemMessageHandler;
import edu.asu.diging.gilesecosystem.util.exceptions.PropertiesStorageException;
import edu.asu.diging.gilesecosystem.util.properties.IPropertiesManager;

@Controller
public class EditPropertiesController {
    
    @Autowired
    private IPropertiesManager propertyManager;

    @Autowired
    private ISystemMessageHandler messageHandler;

    Map<String, String> ocrTypeMap = new HashMap<>();

    @InitBinder
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder, WebDataBinder validateBinder) {
        validateBinder.addValidators(new SystemConfigValidator());
        ocrTypeMap.put(Properties.OCR_PLAINTEXT, propertyManager.getProperty(Properties.OCR_PLAINTEXT));
        ocrTypeMap.put(Properties.OCR_HOCR, propertyManager.getProperty(Properties.OCR_HOCR));
    }

    @RequestMapping(value = "/admin/system/config", method = RequestMethod.GET)
    public String getConfigPage(Model model) {
        SystemConfigPage page = new SystemConfigPage();
        
        page.setGilesAccessToken(propertyManager.getProperty(Properties.GILES_ACCESS_TOKEN));
        page.setBaseUrl(propertyManager.getProperty(Properties.BASE_URL));

        if(propertyManager.getProperty(Properties.TESSERACT_CREATE_HOCR).equalsIgnoreCase("true")) {
            page.setOCRType(Properties.OCR_HOCR);
        } else {
            page.setOCRType(Properties.OCR_PLAINTEXT);
        }

        model.addAttribute("ocrTypes", ocrTypeMap);
        model.addAttribute("systemConfigPage", page);
        return "admin/system/config";
    }
    
    @RequestMapping(value = "/admin/system/config", method = RequestMethod.POST)
    public String storeSystemConfig(@Validated @ModelAttribute SystemConfigPage systemConfigPage, BindingResult results, Model model, RedirectAttributes redirectAttrs) {
        model.addAttribute("systemConfigPage", systemConfigPage);
        
        if (results.hasErrors()) {
            model.addAttribute("show_alert", true);
            model.addAttribute("alert_type", "danger");
            model.addAttribute("alert_msg", "System Configuration could not be saved. Please check the error messages below.");
            return "admin/system/config";
        }
        
        Map<String, String> propertiesMap = new HashMap<String, String>();
        propertiesMap.put(Properties.GILES_ACCESS_TOKEN, systemConfigPage.getGilesAccessToken());
        propertiesMap.put(Properties.BASE_URL, systemConfigPage.getBaseUrl());
        if(systemConfigPage.getOCRType().equals(Properties.OCR_HOCR)) {
            propertiesMap.put(Properties.TESSERACT_CREATE_HOCR, "true");
        } else {
            propertiesMap.put(Properties.TESSERACT_CREATE_HOCR, "false");
        }
        
        try {
            propertyManager.updateProperties(propertiesMap);
        } catch (PropertiesStorageException e) {
            model.addAttribute("show_alert", true);
            model.addAttribute("alert_type", "danger");
            model.addAttribute("alert_msg", "An unexpected error occurred. System Configuration could not be saved.");
            messageHandler.handleMessage("Error while updating System Configuration. System Configuration could not be saved.", e, MessageType.ERROR);
            return "admin/system/config";
        }
        
        redirectAttrs.addFlashAttribute("show_alert", true);
        redirectAttrs.addFlashAttribute("alert_type", "success");
        redirectAttrs.addFlashAttribute("alert_msg", "System Configuration was successfully saved.");
        
        return "redirect:/admin/system/config";
    }
}
