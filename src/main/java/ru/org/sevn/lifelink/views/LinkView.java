/*
 * Copyright 2020 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.lifelink.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ru.org.sevn.lifelink.MainView;
import ru.org.sevn.lifelink.ObjectIndexer;
import ru.org.sevn.lifelink.StoredLink;

@Route(value = "Link", layout = MainView.class)
@PageTitle("Link")
@CssImport("styles/views/link-view.css")
public class LinkView extends Div implements HasUrlParameter<String> {
    private TextField id = new TextField();
    private TextField gid = new TextField();
    private TextField created = new TextField();
    //private NumberField createdDate = new NumberField();
    
    private TextField title = new TextField();
    private TextField url = new TextField();
    private TextField sshotPath = new TextField();
    private Checkbox watchLater = new Checkbox();
    private TextField tags = new TextField();
    private TextArea note = new TextArea();
    private TextField contentPath = new TextField();

    private Button cancel = new Button("Cancel");
    private Button save = new Button("Save");

    private final Binder<StoredLink> binder = new Binder<>(StoredLink.class);
    
    @Autowired
    private ObjectIndexer objectIndexer;
    
    private StoredLink storedLink;

    public LinkView() {
        setId("link-view");
        VerticalLayout wrapper = createWrapper();

        createTitle(wrapper);
        createFormLayout(wrapper);
        createButtonLayout(wrapper);

        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> binder.readBean(storedLink));
        save.addClickListener(e -> {
            if (storedLink != null) {
                try {
                    binder.writeBean(storedLink);
                    objectIndexer.processObject(storedLink);
                    Notification.show("Saved");
                } catch (Exception ex) {
                    Logger.getLogger(LinkView.class.getName()).log(Level.SEVERE, null, ex);
                    Notification.show("Error");
                }
            } else {
                Notification.show("Nothing to save");
            }
        });

        add(wrapper);        
    }
    
    private void createTitle(VerticalLayout wrapper) {
        H1 h1 = new H1("Link");
        wrapper.add(h1);
    }
    
    private VerticalLayout createWrapper() {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setId("wrapper");
        wrapper.setSpacing(false);
        return wrapper;
    }

    private void createButtonLayout(VerticalLayout wrapper) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.addClassName("button-layout");
        buttonLayout.setWidthFull();
        buttonLayout
                .setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(cancel);
        buttonLayout.add(save);
        wrapper.add(buttonLayout);
    }
    
    private FormLayout.FormItem addFormItem(VerticalLayout wrapper,
            FormLayout formLayout, Component field, String fieldName) {
        FormLayout.FormItem formItem = formLayout.addFormItem(field, fieldName);
        wrapper.add(formLayout);
        field.getElement().getClassList().add("full-width");
        return formItem;
    }    
    
    private void createFormLayout(VerticalLayout wrapper) {
        FormLayout formLayout = new FormLayout();

        addFormItem(wrapper, formLayout, id, "id");
        id.setEnabled(false);
        
        addFormItem(wrapper, formLayout, gid, "gid");
        gid.setEnabled(false);
        
        addFormItem(wrapper, formLayout, created, "created");
        created.setEnabled(false);

//        addFormItem(wrapper, formLayout, createdDate, "createdDate");
//        createdDate.setEnabled(false);
        
        addFormItem(wrapper, formLayout, url, "url");
        url.setEnabled(false);
        
        addFormItem(wrapper, formLayout, sshotPath, "sshotPath");
        sshotPath.setEnabled(false);
        
        addFormItem(wrapper, formLayout, contentPath, "contentPath");
        contentPath.setEnabled(false);
        
        addFormItem(wrapper, formLayout, title, "title");
        addFormItem(wrapper, formLayout, watchLater, "watchLater");
        addFormItem(wrapper, formLayout, tags, "tags");
        
        {
            FormLayout.FormItem fi = addFormItem(wrapper, formLayout, note, "note");
            formLayout.setColspan(fi, 2);
        }
    }
    
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        storedLink = null;
        binder.readBean(storedLink);
        
        /*
        if (parameter == null) {
            setText("Welcome anonymous.");
        } else {
            setText(String.format("Welcome %s.", parameter));
        }
        */
        final Location location = event.getLocation();
        final QueryParameters queryParameters = location.getQueryParameters();

        final Map<String, List<String>> parametersMap = queryParameters.getParameters();
        
        if (parametersMap.containsKey("n") && parametersMap.containsKey("v")) {
            try {
                final List<StoredLink> res = objectIndexer.listBy(parametersMap.get("n").get(0), parametersMap.get("v").get(0));
                if (res.size() > 0) {
                    storedLink = res.get(0);
                    binder.readBean(storedLink);
                }
            } catch (Exception ex) {
                Logger.getLogger(LinkView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }    
}
