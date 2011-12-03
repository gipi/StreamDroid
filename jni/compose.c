/*
 * http://gstreamer-devel.966125.n4.nabble.com/How-to-concatenate-video-files-td967232.html
 *  $ gcc `pkg-config --cflags --libs gstreamer-0.10` compose.c -o gst_compose
 */
#include <stdio.h> 
//#include <tchar.h>
#include <gst/gst.h> 
#include <android/log.h>

GstElement *pipeline, *conv, *sink, *comp; 

static gboolean bus_call (GstBus     *bus, GstMessage *msg, gpointer    data) { 
    GMainLoop *loop = (GMainLoop *) data; 

    switch (GST_MESSAGE_TYPE (msg)) { 
        case GST_MESSAGE_EOS: { 
            g_print ("End-of-stream\n"); 

            g_main_loop_quit (loop); 
            break; 
        } 
        case GST_MESSAGE_ERROR: { 
            gchar *debug; 
            GError *err; 

            gst_message_parse_error (msg, &err, &debug); 
            g_free (debug); 

            g_print ("Error: %s\n", err->message); 
            g_error_free (err); 

            g_main_loop_quit (loop); 
            break; 
        } 
        case GST_MESSAGE_STATE_CHANGED: { 
            g_print ("GST_MESSAGE_STATE_CHANGED: \n"); 
            break; 
        } 
        case GST_MESSAGE_SEGMENT_DONE: { 
            g_print ("Bus msg: GST_MESSAGE_SEGMENT_DONE\n"); 

            break; 
        } 
        default: 
        { 
            g_print ("Msg-Type: %d", GST_MESSAGE_TYPE (msg)); 
            break; 
        }     
        break; 
    } 

    return TRUE; 
} 

static void 
comp_new_pad (GstElement *element, 
              GstPad     *pad, 
              gpointer    data) 
{ 
    GstPad *sinkpad; 
    /* We can now link this pad with the  decoder */ 

//    sinkpad = gst_element_get_pad (parser, "sink"); 
// do we have to ask for a compatible pad? 
    sinkpad = gst_element_get_compatible_pad (conv, pad, 
gst_pad_get_caps(pad)); 

    gchar* srcCapsStr = gst_caps_to_string (gst_pad_get_caps(pad) ); 
    gchar* sinkCapsStr = gst_caps_to_string (gst_pad_get_caps(sinkpad) ); 
    //   
    GstPadLinkReturn result = gst_pad_link (pad, sinkpad); 
    if (result == GST_PAD_LINK_OK) 
        g_print ("Dynamic pad created, linking comp/parser\n"); 
    else 
        g_print ("comp_new_pad(): gst_pad_link() failed! result: %d\n", 
result); 
  

    gst_object_unref (sinkpad); 
} 

#define CHECK(e) if (e == NULL) {__android_log_print(ANDROID_LOG_DEBUG, "NDK", #e" is NULL"); return -1;}

int compose(int n, char* filenames[]) {
    GMainLoop *loop; 
    GstBus *bus; 

    /* initialize GStreamer */ 
    gst_init (NULL, NULL); 

    GstRegistry* registry = gst_registry_get_default();
    gst_registry_add_path(registry, "/data/data/org.ktln2.android.streamdroid/lib/");
    GList* paths = gst_registry_get_path_list(registry);

    while (paths) {
	    __android_log_print(ANDROID_LOG_DEBUG, "NDK", "path: '%s'", paths->data);
	    paths = paths->next;
    }

    // without this doesn't find the plugins
    gboolean scan = gst_registry_scan_path(registry, "/data/data/org.ktln2.android.streamdroid/lib/");
    __android_log_print(ANDROID_LOG_DEBUG, "NDK", "scan = %d", scan);

    loop = g_main_loop_new (NULL, FALSE); 
    gst_debug_set_active(TRUE); 
    //gst_debug_set_threshold_for_name ("*", GST_LEVEL_INFO ); // GST_LEVEL_LOG 
    /* create elements */ 
    pipeline = gst_pipeline_new ("TM_video-player"); 
    CHECK(pipeline);

    conv = gst_element_factory_make ("ffmpegcolorspace", "ffmpeg-colorspace"); 
    CHECK(conv);

    sink = gst_element_factory_make ("filesink", "output"); 
    CHECK(sink);
    g_object_set(G_OBJECT(sink),
		    "location", "/data/local/tmp/miao",
		    NULL);

    comp = gst_element_factory_make("gnlcomposition", "mycomposition"); 
    CHECK(comp);

    int cycle;

    GstElement* gnlfilesource[n];

    for (cycle = 0 ; cycle < n ; cycle++) {
	    fprintf(stderr, " + '%s'\n", filenames[cycle]);
	    char videoIdx[10];
	    sprintf(videoIdx, "video%d", cycle);
	    gnlfilesource[cycle] = gst_element_factory_make("gnlfilesource", videoIdx); 
	    gst_bin_add (GST_BIN (comp), gnlfilesource[cycle]); 

	    g_object_set (G_OBJECT (gnlfilesource[cycle]),
			    "location", filenames[cycle], 
			    "start", 5 * GST_SECOND * cycle,
			    "duration", 5 * GST_SECOND,
			    "media-start", 5 * GST_SECOND * cycle,
			    "media-duration", 5 * GST_SECOND,
			    NULL);
    }

    if (!pipeline || !conv || !sink || !comp) { 
	__android_log_print(ANDROID_LOG_DEBUG, "NDK", "One of element could not be created");
        return -1; 
    }

    bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline)); 
    gst_bus_add_watch (bus, bus_call, loop); 
    gst_object_unref (bus); 

    /* put all elements in a bin */ 
    gst_bin_add_many (GST_BIN (pipeline), comp, conv, sink,  NULL); 

    gst_element_link (conv, sink); 

    g_signal_connect (comp, "pad-added", G_CALLBACK (comp_new_pad), NULL); 

    /* Now set to playing and iterate. */ 
	__android_log_print(ANDROID_LOG_DEBUG, "NDK", "Setting to PLAYING\n"); 
    gst_element_set_state (pipeline, GST_STATE_PLAYING); 
    g_print ("Running\n"); 

    g_main_loop_run (loop); 

    /* clean up nicely */ 
    g_print ("Returned, stopping playback\n"); 
    gst_element_set_state (pipeline, GST_STATE_NULL); 
    g_print ("Deleting pipeline\n"); 
    gst_object_unref (GST_OBJECT (pipeline)); 

    return 0; 
} 
