package edu.asu.diging.gilesecosystem.cassiopeia.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;


/**
 * This class is used for getting Beans in any non-managed Spring classes. 
 *
 */
@Service
public class BeanUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    /* (non-Javadoc)
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * Used to get Bean by passing the Bean name as parameter
     * @param beanClass
     * @return class 
     */
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

}