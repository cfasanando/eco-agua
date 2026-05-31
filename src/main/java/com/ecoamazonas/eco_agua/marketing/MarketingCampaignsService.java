package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.blog.BlogPost;
import com.ecoamazonas.eco_agua.blog.BlogPostRepository;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class MarketingCampaignsService {

    private final PromotionService promotionService;
    private final BlogPostRepository blogPostRepository;
    private final TestimonialRepository testimonialRepository;
    private final MarketingCampaignCalendarRepository campaignCalendarRepository;

    public MarketingCampaignsService(
            PromotionService promotionService,
            BlogPostRepository blogPostRepository,
            TestimonialRepository testimonialRepository,
            MarketingCampaignCalendarRepository campaignCalendarRepository
    ) {
        this.promotionService = promotionService;
        this.blogPostRepository = blogPostRepository;
        this.testimonialRepository = testimonialRepository;
        this.campaignCalendarRepository = campaignCalendarRepository;
    }

    @Transactional(readOnly = true)
    public MarketingCampaignsSnapshot buildSnapshot() {
        LocalDate today = LocalDate.now();

        List<MarketingCampaignCalendarItem> calendarItems = safeCampaignCalendarList(campaignCalendarRepository.findAllForAdmin());
        List<Promotion> allPromotions = safePromotionList(promotionService.findAll());
        List<Promotion> activePromotions = allPromotions.stream()
                .filter(this::isPromotionActive)
                .sorted(Comparator
                        .comparing((Promotion promotion) -> promotion.getEndDate() != null ? promotion.getEndDate() : LocalDate.MAX)
                        .thenComparing(Promotion::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        List<BlogPost> allPosts = safeBlogPostList(blogPostRepository.findAll());
        List<BlogPost> publishedPosts = allPosts.stream()
                .filter(post -> post.getStatus() == BlogPost.Status.PUBLISHED)
                .sorted(Comparator.comparing(BlogPost::getPublishedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
        List<BlogPost> draftPosts = allPosts.stream()
                .filter(post -> post.getStatus() == BlogPost.Status.DRAFT)
                .sorted(Comparator.comparing(BlogPost::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        List<Testimonial> allTestimonials = safeTestimonialList(testimonialRepository.findAllByOrderByDisplayOrderAscIdAsc());
        List<Testimonial> activeTestimonials = allTestimonials.stream()
                .filter(Testimonial::isActive)
                .collect(Collectors.toList());

        int promotionsEndingSoonCount = (int) activePromotions.stream()
                .filter(promotion -> promotion.getEndDate() != null)
                .filter(promotion -> !promotion.getEndDate().isBefore(today))
                .filter(promotion -> !promotion.getEndDate().isAfter(today.plusDays(7)))
                .count();

        return new MarketingCampaignsSnapshot(
                today,
                activePromotions.size(),
                publishedPosts.size(),
                draftPosts.size(),
                activeTestimonials.size(),
                promotionsEndingSoonCount,
                buildCampaignRows(calendarItems, activePromotions, publishedPosts, activeTestimonials, today),
                buildPromotionAssets(activePromotions, today),
                buildContentAssets(publishedPosts, draftPosts),
                buildTestimonialAssets(activeTestimonials, allTestimonials),
                buildPendingTasks(calendarItems, activePromotions, publishedPosts, draftPosts, activeTestimonials, today),
                buildSuggestedSegments(),
                buildCtaTemplates()
        );
    }

    private List<MarketingCampaignsSnapshot.CampaignRow> buildCampaignRows(
            List<MarketingCampaignCalendarItem> calendarItems,
            List<Promotion> activePromotions,
            List<BlogPost> publishedPosts,
            List<Testimonial> activeTestimonials,
            LocalDate today
    ) {
        List<MarketingCampaignsSnapshot.CampaignRow> rows = new ArrayList<>();

        for (MarketingCampaignCalendarItem campaign : calendarItems.stream()
                .filter(campaign -> campaign.getStatus() != MarketingCampaignCalendarItem.Status.ARCHIVED)
                .limit(5)
                .collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.CampaignRow(
                    campaign.getName(),
                    campaign.getTypeLabel(),
                    firstNonBlank(campaign.getTargetSegment(), "Público general"),
                    firstNonBlank(campaign.getChannel(), "WhatsApp / redes / portal"),
                    campaign.getStartDate(),
                    campaign.getEndDate(),
                    campaign.getStatusLabel(),
                    firstNonBlank(campaign.getObjective(), "Coordinar campaña con objetivo comercial definido"),
                    firstNonBlank(campaign.getNextAction(), "Definir publicación, diseño o mensaje de WhatsApp"),
                    "/marketing/admin/campaigns?id=" + campaign.getId()
            ));
        }

        for (Promotion promotion : activePromotions.stream().limit(3).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.CampaignRow(
                    promotion.getName(),
                    "Promoción",
                    inferPromotionSegment(promotion),
                    "WhatsApp / catálogo / inicio",
                    promotion.getStartDate(),
                    promotion.getEndDate(),
                    buildPromotionStatus(promotion, today),
                    "Llevar consultas hacia WhatsApp y catálogo",
                    buildPromotionNextAction(promotion, today),
                    "/marketing/admin/promotions"
            ));
        }

        for (BlogPost post : publishedPosts.stream().limit(2).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.CampaignRow(
                    post.getTitle(),
                    "Contenido",
                    inferContentAudience(post),
                    "Blog / WhatsApp / redes",
                    post.getPublishedAt() != null ? post.getPublishedAt().toLocalDate() : null,
                    null,
                    "Publicado",
                    "Usar como contenido de confianza y atracción",
                    "Compartir por WhatsApp y reutilizar en campañas",
                    "/admin/blog"
            ));
        }

        if (!activeTestimonials.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.CampaignRow(
                    "Rotación de prueba social",
                    "Testimonios",
                    "Clientes nuevos / primera compra",
                    "Inicio / WhatsApp / soporte comercial",
                    null,
                    null,
                    "Listo",
                    "Usar testimonios para reducir objeciones",
                    "Destacar un testimonio en el portal público y respuestas de WhatsApp",
                    "/marketing/admin/testimonials"
            ));
        }

        return rows.stream().limit(8).collect(Collectors.toList());
    }

    private List<MarketingCampaignsSnapshot.AssetRow> buildPromotionAssets(List<Promotion> promotions, LocalDate today) {
        List<MarketingCampaignsSnapshot.AssetRow> rows = new ArrayList<>();
        for (Promotion promotion : promotions.stream().limit(6).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    promotion.getName(),
                    buildPromotionStatus(promotion, today),
                    firstNonBlank(promotion.getDescription(), "Promoción lista para mover por WhatsApp y el portal público."),
                    "Abrir promoción",
                    "/marketing/admin/promotions",
                    promotion.getStartDate()
            ));
        }
        return rows;
    }

    private List<MarketingCampaignsSnapshot.AssetRow> buildContentAssets(List<BlogPost> publishedPosts, List<BlogPost> draftPosts) {
        List<MarketingCampaignsSnapshot.AssetRow> rows = new ArrayList<>();
        for (BlogPost post : publishedPosts.stream().limit(4).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    post.getTitle(),
                    "Publicado",
                    firstNonBlank(post.getSummary(), "Contenido publicado listo para apoyar campañas."),
                    "Gestionar post",
                    "/admin/blog",
                    post.getPublishedAt() != null ? post.getPublishedAt().toLocalDate() : null
            ));
        }
        for (BlogPost post : draftPosts.stream().limit(2).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    post.getTitle(),
                    "Borrador",
                    firstNonBlank(post.getSummary(), "Contenido pendiente de revisión antes de publicar."),
                    "Revisar borrador",
                    "/admin/blog",
                    post.getCreatedAt() != null ? post.getCreatedAt().toLocalDate() : null
            ));
        }
        return rows;
    }

    private List<MarketingCampaignsSnapshot.AssetRow> buildTestimonialAssets(List<Testimonial> activeTestimonials, List<Testimonial> allTestimonials) {
        List<MarketingCampaignsSnapshot.AssetRow> rows = new ArrayList<>();
        for (Testimonial testimonial : activeTestimonials.stream().limit(4).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    firstNonBlank(testimonial.getAuthorName(), "Testimonio anónimo"),
                    "Activo",
                    firstNonBlank(testimonial.getContent(), "Activo de confianza del cliente."),
                    "Gestionar testimonio",
                    "/marketing/admin/testimonials",
                    testimonial.getCreatedAt() != null ? testimonial.getCreatedAt().toLocalDate() : null
            ));
        }
        if (rows.isEmpty() && !allTestimonials.isEmpty()) {
            Testimonial testimonial = allTestimonials.get(0);
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    firstNonBlank(testimonial.getAuthorName(), "Testimonio"),
                    "Inactivo",
                    "Hay testimonios registrados, pero ninguno está activo para el portal público.",
                    "Activar testimonio",
                    "/marketing/admin/testimonials",
                    testimonial.getCreatedAt() != null ? testimonial.getCreatedAt().toLocalDate() : null
            ));
        }
        return rows;
    }

    private List<MarketingCampaignsSnapshot.TaskRow> buildPendingTasks(
            List<MarketingCampaignCalendarItem> calendarItems,
            List<Promotion> activePromotions,
            List<BlogPost> publishedPosts,
            List<BlogPost> draftPosts,
            List<Testimonial> activeTestimonials,
            LocalDate today
    ) {
        List<MarketingCampaignsSnapshot.TaskRow> rows = new ArrayList<>();

        if (calendarItems.stream().noneMatch(this::isOpenCalendarCampaign)) {
            rows.add(new MarketingCampaignsSnapshot.TaskRow(
                    "Alta",
                    "Planificar campaña en calendario",
                    "Registra al menos una campaña planificada o activa para ordenar publicaciones, canales y mensajes.",
                    "Crear campaña",
                    "/marketing/admin/campaigns"
            ));
        }

        calendarItems.stream()
                .filter(campaign -> campaign.getStatus() == MarketingCampaignCalendarItem.Status.PREPARING)
                .findFirst()
                .ifPresent(campaign -> rows.add(new MarketingCampaignsSnapshot.TaskRow(
                        "Media",
                        "Campaña en preparación",
                        "Completa la campaña '" + campaign.getName() + "' y actívala cuando ya tenga mensaje, canal y siguiente acción.",
                        "Abrir campaña",
                        "/marketing/admin/campaigns?id=" + campaign.getId()
                )));

        activePromotions.stream()
                .filter(promotion -> promotion.getEndDate() != null)
                .filter(promotion -> !promotion.getEndDate().isBefore(today))
                .filter(promotion -> !promotion.getEndDate().isAfter(today.plusDays(7)))
                .findFirst()
                .ifPresent(promotion -> rows.add(new MarketingCampaignsSnapshot.TaskRow(
                        "Alta",
                        "Promoción por vencer",
                        "Revisa y refuerza la promoción '" + promotion.getName() + "' antes de que termine.",
                        "Abrir promociones",
                        "/marketing/admin/promotions"
                )));

        activePromotions.stream()
                .filter(promotion -> isBlank(promotion.getBannerImagePath()))
                .findFirst()
                .ifPresent(promotion -> rows.add(new MarketingCampaignsSnapshot.TaskRow(
                        "Media",
                        "Promoción sin banner",
                        "Agrega un banner a '" + promotion.getName() + "' para que destaque en el portal público y canales comerciales.",
                        "Editar promoción",
                        "/marketing/admin/promotions"
                )));

        if (!draftPosts.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.TaskRow(
                    "Media",
                    "Contenido en borrador pendiente",
                    "Tienes " + draftPosts.size() + " post(s) en borrador para revisar y publicar.",
                    "Abrir blog",
                    "/admin/blog"
            ));
        }

        if (publishedPosts.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.TaskRow(
                    "Alta",
                    "No hay contenido publicado",
                    "Publica al menos un post para apoyar confianza y atracción de campañas.",
                    "Crear post",
                    "/admin/blog/new"
            ));
        }

        if (activeTestimonials.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.TaskRow(
                    "Media",
                    "No hay testimonios activos",
                    "Activa testimonios para usar prueba social en el portal público y conversaciones de WhatsApp.",
                    "Abrir testimonios",
                    "/marketing/admin/testimonials"
            ));
        }

        if (rows.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.TaskRow(
                    "Normal",
                    "Marketing está ordenado",
                    "Mantén campañas, contenido y testimonios alineados con el enfoque comercial de esta semana.",
                    "Revisar campañas",
                    "/marketing/admin/campaigns"
            ));
        }

        return rows;
    }

    private List<String> buildSuggestedSegments() {
        return List.of(
                "Clientes nuevos - primera compra",
                "Clientes próximos a recomprar",
                "Clientes inactivos para reactivar",
                "Bodegas y tiendas",
                "Restaurantes y negocios de comida",
                "Delivery por zona"
        );
    }

    private List<MarketingCampaignsSnapshot.CtaTemplateRow> buildCtaTemplates() {
        return List.of(
                new MarketingCampaignsSnapshot.CtaTemplateRow(
                        "Primera compra",
                        "Clientes nuevos / hogares",
                        "Hola, tenemos productos disponibles para pedido por WhatsApp. ¿Deseas que te enviemos catálogo, disponibilidad y coordinación de entrega?"
                ),
                new MarketingCampaignsSnapshot.CtaTemplateRow(
                        "Recordatorio de recompra",
                        "Clientes frecuentes",
                        "Hola, estamos tomando pedidos para esta semana. ¿Deseas separar tus productos y coordinar entrega por WhatsApp?"
                ),
                new MarketingCampaignsSnapshot.CtaTemplateRow(
                        "Reactivación de cliente",
                        "Clientes inactivos",
                        "Hola, esta semana tenemos campaña activa y atención por WhatsApp. ¿Te enviamos las opciones disponibles?"
                )
        );
    }

    private boolean isOpenCalendarCampaign(MarketingCampaignCalendarItem campaign) {
        if (campaign == null || campaign.getStatus() == null) {
            return false;
        }
        return campaign.getStatus() == MarketingCampaignCalendarItem.Status.PLANNED
                || campaign.getStatus() == MarketingCampaignCalendarItem.Status.PREPARING
                || campaign.getStatus() == MarketingCampaignCalendarItem.Status.ACTIVE;
    }

    private boolean isPromotionActive(Promotion promotion) {
        if (promotion == null || !promotion.isEnabled()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (promotion.getStartDate() != null && promotion.getStartDate().isAfter(today)) {
            return false;
        }
        return promotion.getEndDate() == null || !promotion.getEndDate().isBefore(today);
    }

    private String buildPromotionStatus(Promotion promotion, LocalDate today) {
        if (!promotion.isEnabled()) {
            return "Desactivada";
        }
        if (promotion.getStartDate() != null && promotion.getStartDate().isAfter(today)) {
            return "Programada";
        }
        if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(today)) {
            return "Vencida";
        }
        if (promotion.getEndDate() != null && !promotion.getEndDate().isAfter(today.plusDays(7))) {
            return "Por vencer";
        }
        return "Activa";
    }

    private String buildPromotionNextAction(Promotion promotion, LocalDate today) {
        if (promotion.getEndDate() != null && !promotion.getEndDate().isAfter(today.plusDays(7))) {
            return "Impulsar esta promoción por WhatsApp y portal público antes de que termine";
        }
        if (isBlank(promotion.getBannerImagePath())) {
            return "Agregar banner y destacarla en el portal público";
        }
        return "Mantener visible en WhatsApp y canales de atención";
    }

    private String inferPromotionSegment(Promotion promotion) {
        String text = (firstNonBlank(promotion.getName(), "") + " " + firstNonBlank(promotion.getDescription(), "")).toLowerCase(Locale.ROOT);
        if (text.contains("restaurant") || text.contains("restaurante") || text.contains("bar")) {
            return "Restaurantes y negocios de comida";
        }
        if (text.contains("bodega") || text.contains("store") || text.contains("tienda")) {
            return "Tiendas y bodegas";
        }
        if (text.contains("famil") || text.contains("hogar") || text.contains("casa")) {
            return "Familias y hogares";
        }
        return "Público general / campaña comercial";
    }

    private String inferContentAudience(BlogPost post) {
        String text = (firstNonBlank(post.getTitle(), "") + " " + firstNonBlank(post.getSummary(), "")).toLowerCase(Locale.ROOT);
        if (text.contains("children") || text.contains("hijos") || text.contains("famil")) {
            return "Familias y hogares";
        }
        if (text.contains("restaurant") || text.contains("restaurante") || text.contains("negocio")) {
            return "Restaurantes y negocios";
        }
        return "Campañas de confianza y atracción";
    }

    private String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<Promotion> safePromotionList(List<?> promotions) {
        return promotions == null ? List.of() : (List<Promotion>) promotions;
    }

    @SuppressWarnings("unchecked")
    private List<BlogPost> safeBlogPostList(List<?> posts) {
        return posts == null ? List.of() : (List<BlogPost>) posts;
    }

    @SuppressWarnings("unchecked")
    private List<Testimonial> safeTestimonialList(List<?> testimonials) {
        return testimonials == null ? List.of() : (List<Testimonial>) testimonials;
    }

    @SuppressWarnings("unchecked")
    private List<MarketingCampaignCalendarItem> safeCampaignCalendarList(List<?> campaigns) {
        return campaigns == null ? List.of() : (List<MarketingCampaignCalendarItem>) campaigns;
    }
}
