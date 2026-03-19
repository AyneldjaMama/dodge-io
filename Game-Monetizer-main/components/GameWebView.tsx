import { useRef, useMemo, useImperativeHandle, forwardRef, useEffect, useState } from "react";
import { Platform, StyleSheet, View, Text } from "react-native";
import Colors from "@/constants/colors";

export interface GameWebViewRef {
  postMessage: (msg: string) => void;
}

interface GameWebViewProps {
  html: string;
  onMessage: (event: { nativeEvent: { data: string } }) => void;
  style?: any;
}

const GameWebView = forwardRef<GameWebViewRef, GameWebViewProps>(
  ({ html, onMessage, style }, ref) => {
    if (Platform.OS === "web") {
      return (
        <WebIframe ref={ref} html={html} onMessage={onMessage} style={style} />
      );
    }

    return (
      <NativeWebView ref={ref} html={html} onMessage={onMessage} style={style} />
    );
  }
);

GameWebView.displayName = "GameWebView";
export default GameWebView;

const WebIframe = forwardRef<GameWebViewRef, GameWebViewProps>(
  ({ html, onMessage, style }, ref) => {
    const iframeRef = useRef<any>(null);
    const onMessageRef = useRef(onMessage);
    onMessageRef.current = onMessage;

    useImperativeHandle(ref, () => ({
      postMessage: (msg: string) => {
        iframeRef.current?.contentWindow?.postMessage(msg, "*");
      },
    }));

    useEffect(() => {
      const handler = (event: MessageEvent) => {
        if (typeof event.data === "string") {
          try {
            JSON.parse(event.data);
            onMessageRef.current({ nativeEvent: { data: event.data } });
          } catch {}
        }
      };
      window.addEventListener("message", handler);
      return () => window.removeEventListener("message", handler);
    }, []);

    const blobUrl = useMemo(() => {
      const blob = new Blob([html], { type: "text/html" });
      return URL.createObjectURL(blob);
    }, [html]);

    useEffect(() => {
      return () => {
        URL.revokeObjectURL(blobUrl);
      };
    }, [blobUrl]);

    return (
      <View style={[{ flex: 1 }, style]}>
        <iframe
          ref={iframeRef}
          src={blobUrl}
          style={{
            width: "100%",
            height: "100%",
            border: "none",
            backgroundColor: Colors.background,
          } as any}
          allow="autoplay"
        />
      </View>
    );
  }
);

WebIframe.displayName = "WebIframe";

const NativeWebView = forwardRef<GameWebViewRef, GameWebViewProps>(
  ({ html, onMessage, style }, ref) => {
    const WebView = require("react-native-webview").default;
    const webViewRef = useRef<any>(null);
    const [crashed, setCrashed] = useState(false);

    useImperativeHandle(ref, () => ({
      postMessage: (msg: string) => {
        webViewRef.current?.postMessage(msg);
      },
    }));

    if (crashed) {
      // Re-render WebView after renderer process was killed
      setCrashed(false);
    }

    return (
      <WebView
        ref={webViewRef}
        source={{ html }}
        style={[styles.webview, style]}
        onMessage={onMessage}
        javaScriptEnabled
        scrollEnabled={false}
        bounces={false}
        overScrollMode="never"
        showsHorizontalScrollIndicator={false}
        showsVerticalScrollIndicator={false}
        allowsInlineMediaPlayback
        mediaPlaybackRequiresUserAction={false}
        onRenderProcessGone={() => {
          // Android kills WebView renderer under memory pressure — recover instead of crashing
          console.warn("[GameWebView] Renderer process gone, recovering...");
          webViewRef.current = null;
          setCrashed(true);
        }}
        onError={(syntheticEvent: any) => {
          console.warn("[GameWebView] Error:", syntheticEvent.nativeEvent?.description);
        }}
        onContentProcessDidTerminate={() => {
          // iOS equivalent of onRenderProcessGone
          console.warn("[GameWebView] Content process terminated, recovering...");
          webViewRef.current = null;
          setCrashed(true);
        }}
      />
    );
  }
);

NativeWebView.displayName = "NativeWebView";

const styles = StyleSheet.create({
  webview: {
    flex: 1,
    backgroundColor: Colors.background,
  },
});
