package ru.org.sevn.lifelink.views;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.org.sevn.lifelink.MainView;
import ru.org.sevn.lifelink.ObjectIndexer;
import ru.org.sevn.lifelink.StoredLink;
@Route(value = "Dashboard", layout = MainView.class)
@RouteAlias(value = "va", layout = MainView.class)
@PageTitle("Dashboard")
@CssImport("styles/views/dashboard-view.css")
public class DashboardView extends Div implements AfterNavigationObserver {

    @Autowired
    private ObjectIndexer objectIndexer;

    private final Grid<StoredLink> grid;

    public DashboardView() {
        setId("dashboard-view");
        grid = new Grid<>();
        grid.setId("list");
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_NO_ROW_BORDERS);
        grid.setHeightFull();
        grid.addColumn(new ComponentRenderer<>(obj -> {
            H3 h3 = new H3(
                    obj.getTitle());
            Anchor anchor = new Anchor(obj.getUrl(), "url");
            anchor.getElement().getThemeList().add("font-size-xs");
            Div div = new Div(h3, anchor);
            div.addClassName("obj-column");
            return div;
        }));

        add(grid);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        try {
            grid.setItems(objectIndexer.listBy("id", "*"));
        } catch (final Exception ex) {
            Logger.getLogger(DashboardView.class.getName()).log(Level.SEVERE, null, ex);
            Notification.show("Error data loading");
        }
    }
}
